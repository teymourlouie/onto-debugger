package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.alignments.AlignmentGenerator;
import ir.ac.ui.ontodebugger.alignments.AlignmentLoader;
import ir.ac.ui.ontodebugger.alignments.EntityResolver;
import ir.ac.ui.ontodebugger.errordetectors.*;
import ir.ac.ui.ontodebugger.evaluation.ErrorSetEvaluator;
import ir.ac.ui.ontodebugger.evaluation.Evaluation;
import ir.ac.ui.ontodebugger.mups.BugFinder;
import ir.ac.ui.ontodebugger.mups.DFHitSetBugFinder;
import ir.ac.ui.ontodebugger.mups.HitSetBugFinder;
import ir.ac.ui.ontodebugger.mups.PDFHitSetBugFinder;
import ir.ac.ui.ontodebugger.util.MultiThreadProcess;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import ir.ac.ui.ontodebugger.util.Renderer;
import ir.ac.ui.ontodebugger.util.Timer;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/11/15
 */
public class OntoDebugger {
    private static final Logger LOGGER = LogManager.getLogger(OntoDebugger.class);
    public static final IRI MERGED_ONTOLOGY_IRI = IRI.create(Configs.IRI_PREFIX, "/merged/");

    @Getter
    private final KnowledgeBaseProfile profile;
    private final ErrorSetEvaluator errorSetEvaluator;

    private final ProfileSupportChecker profileSupportChecker = new ProfileSupportChecker();

    private OWLOntology buggyOntology;
    private String buggyOntologyPath = "";


    @Getter
    private List<AxiomRanker> axiomRankers;
    private OWLOntology alignment;

    public OntoDebugger(@Nonnull String knowledgeBasePath) throws OWLOntologyCreationException {
        this.errorSetEvaluator = Configs.getInstance().getEVALUATOR();
        profile = new KnowledgeBaseProfile(knowledgeBasePath);

        axiomRankers = new ArrayList<>();
        for (String s : Configs.getInstance().getAXIOM_RANKER_METHODS()) {
            s = s.trim();
            if ("ShapleyMI".equalsIgnoreCase(s)) {
                axiomRankers.add(new ShapleyMIAxiomRanker());
            } else if ("ProfileSupport".equalsIgnoreCase(s)) {
                axiomRankers.add(new ProfileSupportAxiomRanker(profileSupportChecker, 1000));
            } else if ("ProfileSupport+Shapley".equalsIgnoreCase(s)) {
                axiomRankers.add(new ProfileSupportShapleyMIAxiomRanker(profileSupportChecker, 1000));
            } else if ("ShapleySupport".equalsIgnoreCase(s)) {
                axiomRankers.add(new ShapleySupportAxiomRanker(profileSupportChecker, 1000));
            } else if ("SWOOP".equalsIgnoreCase(s)) {
                axiomRankers.add(new SwoopAxiomRanker());
            } else if ("InformationContent".equalsIgnoreCase(s)) {
                axiomRankers.add(new InformationContentAxiomRanker());
            } else {
                LOGGER.error("Axiom Ranker '{}' is not supported yet!", s);
            }
        }
    }

    public void debug(String filepath) {
        switch (Configs.getInstance().getDEBUGGING_TASK()) {
            case ONTOLOGY_DEBUGGING:
                debugOntology(filepath);
                break;
            case ALIGNMENT_DEBUGGING:
                debugAlignment(filepath);
                break;
            default:
                LOGGER.error("Debugging Task {} is not supported yet!", Configs.getInstance().getDEBUGGING_TASK());
        }
    }

    private void debugOntology(@Nonnull String ontologyPath) {
        final OWLOntology ontology = OntologyHelper.loadOntology(ontologyPath);
        if (ontology == null) {
            LOGGER.error("Ontology is null!!!");
            return;
        }

        debug(ontology, ontologyPath);
    }

    private void debugAlignment(@Nonnull String alignmentPath) {
        AlignmentLoader loader = new AlignmentLoader(alignmentPath);
        final OWLOntology alignedOntology = loader.getOWLOntology(new EntityResolver(profile.getProfileOntology()), 0.30);
        if (alignedOntology == null) {
            LOGGER.error("Ontology loaded from alignment is null!!!");
            return;
        }

        // debug ontology equivalent to the alignment
        debug(alignedOntology, alignmentPath);
    }

    public void debug(@Nonnull OWLOntology ontology, String ontologyPath) {
        cleanup(buggyOntology);
        cleanup(alignment);
        buggyOntology = ontology;
        buggyOntologyPath = ontologyPath;

        Timer debugTimer = Timer.start("Debugger");
        LOGGER.info("Debug started at {}", System.currentTimeMillis());

        if (Configs.getInstance().isUSE_ALIGNMENTS()) {
            alignment = loadAlignments();
        }
        loadExtraOntology();

        // init profile support checker
        profileSupportChecker.init(profile, alignment);

        // pre-process error list and white list
        Set<OWLAxiom> initialErrors = new HashSet<>();
        Set<OWLAxiom> whiteList = new HashSet<>();

        if (Configs.getInstance().isASSUME_PROFILE_AXIOMS_ARE_CORRECT() && profile.getProfileOntology() != null) {
            whiteList.addAll(profile.getProfileOntology().getAxioms());
        }

        if (Configs.getInstance().isASSUME_ALIGNMENT_AXIOMS_ARE_CORRECT() && alignment != null) {
            whiteList.addAll(alignment.getAxioms());
        }

        ProfileAwareErrorPreprocessor preprocessor = new ProfileAwareErrorPreprocessor();
        if (Configs.getInstance().isPREPROCESS_ERRORS() || Configs.getInstance().isDIRECT_ERROR_DETECTION()) {
            final String methodName = "Direct Error Detection";
            Timer preprocessTimer = Timer.start(methodName);

            preprocessor.init(profile, alignment, buggyOntology.getAxioms());
            initialErrors.addAll(preprocessor.getErrors());
            whiteList.addAll(preprocessor.getWhiteList());

            preprocessTimer.stopAndPrint();
            evaluateErrors(methodName, preprocessor.getErrors(), preprocessTimer.getElapsedTimeMillis());
        }

        if (!Configs.getInstance().isDIRECT_ERROR_DETECTION()) {
            BugList localBugs = null;
            BugList mergedBugs = null;
            if (Configs.getInstance().isDEBUG_ONTOLOGY_LONELY()) {
                localBugs = doRealDebugging(buggyOntology, initialErrors, whiteList, buggyOntologyPath);
            }

            if (Configs.getInstance().isDEBUG_MERGED_ONTOLOGY()) {
                OWLOntology mergedOntology = getMergedOntology();
                if (mergedOntology == null) {
                    LOGGER.error("Merged ontology should not be null!!!");
                    return;
                }

                if (localBugs != null) {
                    final List<OWLAxiom> notExistedInMerged = localBugs.getSuspectedAxioms().stream().filter(owlAxiom -> !mergedOntology.containsAxiom(owlAxiom)).collect(Collectors.toList());
                    if (!notExistedInMerged.isEmpty()) {
                        LOGGER.info("Axioms that are missing in merged Ontology: ");
                        LOGGER.info(Renderer.render(notExistedInMerged));
                        LOGGER.info("++++++++++++++++++++++++++++++++++++++++++++++++++");
                    }
                }

                mergedBugs = doRealDebugging(mergedOntology, initialErrors, whiteList, buggyOntologyPath + "-merged");
                cleanup(mergedOntology);
            }
        }
        // clean up section
        profileSupportChecker.fini();
        cleanup(buggyOntology);
        cleanup(alignment);

        buggyOntology = null;
        alignment = null;

        debugTimer.stopAndPrint();
    }

    public BugList doRealDebugging(@Nonnull OWLOntology ontology, Set<OWLAxiom> initialErrors, Set<OWLAxiom> whiteList, String ontologyPath) {
        final String baseName = FilenameUtils.getBaseName(ontologyPath);

        OWLOntology onto2debugged = ontology;

        OWLOntology cachedBugListOntology = null;
        final String cachedOntologyPath = OntologyHelper.getCachedOntologyPath(ontologyPath);
        if (Configs.getInstance().isUSE_CACHED_ONTOLOGY()) {
            cachedBugListOntology = OntologyHelper.loadOntology(cachedOntologyPath);
            if (cachedBugListOntology != null) {
                onto2debugged = cachedBugListOntology;
            } else {
                LOGGER.error("Cached BugList ontology could not be loaded!");
            }
        }

        //step 2: detect Bugs based on MUPS
        BugList bugList;

        onto2debugged.getOWLOntologyManager().removeAxioms(onto2debugged, initialErrors);
        bugList = getBugList(onto2debugged);

        if (!bugList.isEmpty()) {
            bugList.getBlackList().addAll(initialErrors);
            bugList.getWhiteList().addAll(whiteList);

            // save MUPS as cached error ontology
            if (Configs.getInstance().isSAVE_CACHED_ONTOLOGY()) {
                bugList.saveAsOntology(cachedOntologyPath);
            }

            printBugList2File(bugList, baseName);

            evaluateErrors("AllSuspectedAxioms", bugList.getSuspectedAxioms(), 0);
            if (Configs.getInstance().isFIND_ROOT_ERRORS()) {
                axiomRankers.stream().forEach(axiomRanker -> findErrors(axiomRanker, ontology, bugList));
            }
        }

        cleanup(cachedBugListOntology);
        return bugList;
    }

    private void printBugList2File(BugList bugList, String ontoName) {
        String filename = FilenameUtils.concat(Configs.getInstance().getRESULT_PATH(), ontoName + "-bugs.txt");

        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("Bugs in ontology:" + ontoName);
            writer.println("######################################################################");
            bugList.getBugs().stream().sorted().forEach(bug -> {
                writer.println(bug.toString());
                writer.println();
                writer.println("MUPS Axioms:");
                bug.getAxioms().stream().sorted().forEach(axiom -> {
                    boolean isInBuggy = buggyOntology.containsAxiom(axiom);
                    boolean isInProfile = profile.getProfileOntology().containsAxiom(axiom);
                    boolean isInAlignment = alignment == null ? false : alignment.containsAxiom(axiom);

                    writer.println(isInBuggy + "," + isInProfile + "," + isInAlignment + "\t" + Renderer.render(axiom));
                });

                writer.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                bug.getMupsSet().stream().sorted().forEach(mups -> {
                    writer.println(mups);
                    writer.println("++++++++++++++++++++++++++++++++++++++++++");
                });

                writer.println("*************************************************************************************");
            });
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }

    private void loadExtraOntology() {
        // if extra ontology to help alignment generation is specified in configs, then use it
        final File ontoExtra = Configs.getInstance().getONTO_EXTRA();
        if (ontoExtra != null) {
            final OWLOntology extraOntology = OntologyHelper.loadOntology(ontoExtra);
            if (extraOntology != null) {
                buggyOntology.getOWLOntologyManager().addAxioms(extraOntology, extraOntology.getAxioms());
            }
        }
    }


    private void cleanup(OWLOntology ontology) {
        if (ontology != null) {
            ontology.getOWLOntologyManager().removeOntology(ontology);
        }
    }

    private void evaluateErrors(String detectorName, Set<OWLAxiom> errors, long time) {
        if (Configs.getInstance().isEVALUATE_RESULTS() && errorSetEvaluator != null) {
            final Evaluation evaluation = errorSetEvaluator.evaluate(buggyOntology, errors);
            LOGGER.info("{} -> {}\t{}", String.format("%-40s", detectorName), evaluation, time);
            LOGGER.info("Correct Axioms detected as Error:\n{}", Renderer.render(errorSetEvaluator.getFalseAxioms()));

        }
    }

    private BugList getBugList(OWLOntology ont) {
        Timer timer = Timer.start("Finding MUPSs");

        BugFinder bugFinder;
        switch (Configs.getInstance().getBUG_FINDER_METHOD()) {
            case DF_HITSET:
                bugFinder = new DFHitSetBugFinder(profile, buggyOntology);
                break;
            case PARALLEL_DF_HITSET:
                bugFinder = new PDFHitSetBugFinder(profile, buggyOntology);
                break;
            case HITSET:
                bugFinder = new HitSetBugFinder(profile, buggyOntology, false, false);
                break;
            case PARALLEL_HITSET:
            default:
                bugFinder = new HitSetBugFinder(profile, buggyOntology, true, false);
                break;

        }

        List<Bug> bugs = bugFinder.findBugs(ont);

        timer.stopAndPrint();

        BugList bugList = new BugList(bugs);
        if (!bugList.isEmpty()) {
            LOGGER.info("************************************* Bugs Detected ********************************************");
            bugList.printInfo();
            LOGGER.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
        return bugList;
    }

    private Set<OWLAxiom> findErrors(AxiomRanker axiomRanker, OWLOntology ont, BugList bugs) {
        Timer timer = Timer.start("Finding Root Errors using " + axiomRanker);
        ErrorDetector detector = new ErrorDetector(axiomRanker);
        final Set<OWLAxiom> errors = detector.findErrors(ont, bugs);
        timer.stopAndPrint();

        LOGGER.info("***************************** Errors Detected by {} **********************************", axiomRanker);
        LOGGER.info("{}", Renderer.render(errors));
        evaluateErrors(axiomRanker.toString(), errors, timer.getTotal());
        LOGGER.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        return errors;
    }

    private OWLOntology loadAlignments() {
        Timer timer = Timer.start("Loading Alignments");

        OWLOntology alignments;
        if (Configs.getInstance().getALIGN_PATH() != null) {
            alignments = loadAlignmentsFromPath();
        } else {
            alignments = generateAlignments();
        }

        LOGGER.info("Alignment Ontology: {}", OntologyHelper.getOntologyInfo(alignments));
        LOGGER.info("Axioms included in Alignment:", Renderer.render(alignments.getAxioms()));
        timer.stopAndPrint();
        return alignments;
    }

    private OWLOntology loadAlignmentsFromPath() {

        File filePath = new File(Configs.getInstance().getALIGN_PATH());
        if (!filePath.exists()) {
            LOGGER.error("ALIGN_PATH is not existed!");
            return null;
        }

        OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();
        try {
            OWLOntology alignments = manager.createOntology();
            if (filePath.isFile()) { //debug single ontologies
                final OWLOntology loadedOntology = loadAlignmentFromFile(filePath);
                if (loadedOntology != null) {
                    alignments.getOWLOntologyManager().addAxioms(alignments, loadedOntology.getAxioms());
                }
            } else if (filePath.isDirectory()) { // load directory of alignments
                OntologyHelper.getListOfAlignmentFiles(filePath.getPath())
                        .stream().filter(file -> !Configs.getInstance().getALIGN_EXCLUDED().contains(file.getPath()))
                        .forEachOrdered(file -> {
                            final OWLOntology loadedOntology = loadAlignmentFromFile(file);
                            if (loadedOntology != null) {
                                alignments.getOWLOntologyManager().addAxioms(alignments, loadedOntology.getAxioms());
                            }
                        });
            }
            return alignments;
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }
        return null;
    }

    private OWLOntology generateAlignments() {
        OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();
        try {
            OWLOntology alignments = manager.createOntology();

            profile.getOntologyList().stream().forEach(target -> {
                final OWLOntology align = loadAlignment(target);
                if (align != null) {
                    alignments.getOWLOntologyManager().addAxioms(alignments, align.getAxioms());
                }
            });
            return alignments;
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }
        return null;
    }

    private OWLOntology loadAlignmentFromFile(@Nonnull File file) {
        AlignmentLoader loader = new AlignmentLoader(file);
        return loader.getOWLOntology(new EntityResolver(profile.getProfileOntology(), buggyOntology), Configs.getInstance().getALIGNMENT_ACCEPTANCE_THRESHOLD());
    }

    private OWLOntology loadAlignment(OWLOntology target) {
        if (target == null || buggyOntology == null) {
            LOGGER.error("For loading alignments, both of ontologies should be not null. target:{}, buggyOntology: {}"
                    , target, buggyOntology);
            return null;
        }

        OWLOntology alignmentOntology = null;
        final double threshold = Configs.getInstance().getALIGNMENT_ACCEPTANCE_THRESHOLD();

        try {
            AlignmentGenerator generator = new AlignmentGenerator(buggyOntology, target);
            String targetNameSpace = profile.getOntologyFileName(target);
            String alignmentCachePath = OntologyHelper.getCachedAlignmentPath(buggyOntologyPath, targetNameSpace);

            alignmentOntology = generator.loadAlignment(alignmentCachePath, threshold);
            if (alignmentOntology == null) {
                alignmentOntology = (OWLOntology) MultiThreadProcess.runAndWaitWithTimeout(
                        () -> generator.generate(threshold)
                        , 5, TimeUnit.HOURS);
                if (alignmentOntology != null) {
                    generator.saveAs(alignmentCachePath);
                }
            }
        } catch (Exception e) {
            LOGGER.catching(e);
        }
        return alignmentOntology;
    }

    /***
     * Merge ontologies loaded into manager and set the IRI on the merged ontology to the given one
     */
    private OWLOntology getMergedOntology() {
        Timer timer = Timer.start("Merging");
        // Create our ontology merger
        OWLOntology merged;
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            merged = manager.createOntology(MERGED_ONTOLOGY_IRI);
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
            return null;
        }

        manager.addAxioms(merged, buggyOntology.getAxioms());
        if (profile != null && profile.getProfileOntology() != null) {
            manager.addAxioms(merged, profile.getProfileOntology().getAxioms());
        }

        if (alignment != null) {
            manager.addAxioms(merged, alignment.getAxioms());
        }


        LOGGER.info("Merged Ontology:{}", OntologyHelper.getOntologyInfo(merged));
        timer.stopAndPrint();
        return merged;
    }
}
