package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.Bug;
import ir.ac.ui.ontodebugger.Configs;
import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.MultiThreadProcess;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import ir.ac.ui.ontodebugger.util.Renderer;
import ir.ac.ui.ontodebugger.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/11/15
 */
public abstract class BugFinder {
    private static final Logger LOGGER = LogManager.getLogger(BugFinder.class);
    private final KnowledgeBaseProfile profile;
    protected final OWLOntology ontology;

    private final AtomicInteger remainedCounter = new AtomicInteger();
    private final AtomicInteger activeAnalysisCounter = new AtomicInteger();
    private final MUPSFinder mupsFinder;

    /**
     * Construct a Bug Finder
     * both parameters are used to determine type of MUPS found by the BugFinder
     *
     * @param profile  profile ontology
     * @param ontology original ontology
     */
    public BugFinder(KnowledgeBaseProfile profile, OWLOntology ontology) {
        this.profile = profile;
        this.ontology = ontology;

        switch (Configs.getInstance().getMUPS_FINDER_METHOD()) {
            case SWOOP:
            default:
                mupsFinder = new SwoopMUPSFinder();
                break;
        }
    }

    /**
     * Find Bugs related to the given signature within an ontology
     *
     * @param ont
     * @return List of Bugs detected
     */
    public List<Bug> findBugs(OWLOntology ont) {
        Set<OWLEntity> unsatEntities = new HashSet<>();
        Map<OWLAxiom, OWLAxiom> propertyClassAxioms = new ConcurrentHashMap<>();

        OWLReasoner reasoner = ReasonerFactory.getReasoner(ont);


        if (Configs.getInstance().isDEBUG_CLASSES()) {
            final Set<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
            LOGGER.info("{} unsatisfiable classes in the ontology", unsatisfiableClasses.size());
            unsatEntities.addAll(unsatisfiableClasses);
        }

        if (Configs.getInstance().isDEBUG_PROPERTIES()) {
            final Set<OWLObjectPropertyExpression> unsatisfiableProperties = reasoner.getEquivalentObjectProperties(OWLManager.getOWLDataFactory().getOWLBottomObjectProperty()).getEntitiesMinusBottom();
            LOGGER.info("{} unsatisfiable properties in the ontology", unsatisfiableProperties.size());
            unsatisfiableProperties.stream().map(OWLObjectPropertyExpression::getNamedProperty).forEach(unsatEntities::add);
        }

        LOGGER.info("Unsatisfiable Entities: {}", unsatEntities.size());
        remainedCounter.set(unsatEntities.size());

        // don't return bugs with empty diagnosis, which means there is no axiom from buggy ontology in it
        return findBugs(ont, unsatEntities, propertyClassAxioms).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected List<Bug> findBugs(OWLOntology ont, Set<OWLEntity> unsatEntities, Map<OWLAxiom, OWLAxiom> propertyClassAxioms) {
        List<Bug> bugs = new ArrayList<>();
        if (Configs.getInstance().isSINGLE_THREAD_REASONING()) {
            unsatEntities.stream().map(cla -> getBug(ont, propertyClassAxioms, cla)).forEach(bugs::add);
        } else {
            MultiThreadProcess.runAndWait(() ->
                    unsatEntities.parallelStream().map(cla -> getBug(ont, propertyClassAxioms, cla)).forEach(bugs::add));
        }

        return bugs;
    }

    protected Bug getBug(OWLOntology ont, Map<OWLAxiom, OWLAxiom> propertyClassAxioms, OWLEntity entity) {
        return getBug(ont, propertyClassAxioms, entity, Collections.emptySet());
    }

    protected Bug getBug(OWLOntology ont, Map<OWLAxiom, OWLAxiom> propertyClassAxioms, OWLEntity entity, Set<MUPS> cache) {
        final Timer timer = Timer.start("Find Bug " + entity);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology clonedOntology = null;
        Bug bug = null;

        // uncomment to add some entities to ignore list
        //TODO: comment excluding code before commit
        /*final String s = entity.toString();
        if (!s.equals("<http://dbpedia.org/ontology/Library>")){
            return Bug.emptyBug(entity);
        }*/
//        if (s.equals("<http://www.wikidata.org/entity/Q2354973>")
//                || s.equals("<http://dbpedia.org/ontology/RoadTunnel>")
//                || s.equals("<http://dbpedia.org/ontology/WaterwayTunnel>")) {
//            return null;
//        }

        PerformanceLog log = new PerformanceLog();

        try {
            clonedOntology = manager.createOntology(getAxiomsRelatedToUnsatClass(ont, entity));
            LOGGER.debug("Modular Ontology related to Entity {} :{}", entity, OntologyHelper.getOntologyInfo(clonedOntology));

            LOGGER.info("Analysing unsatisfiable {}:{} started... ActiveAnalysis: {}", entity.getClass().getSimpleName(), entity, activeAnalysisCounter.incrementAndGet());
            bug = findBug(clonedOntology, entity, cache, log);
            if (bug != null) {
                // replace fake class definition axioms with the real property domain/range axioms
                bug.replaceAxioms(propertyClassAxioms);

                LOGGER.info("New Bug detected: {}", bug);
            }
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }
        if (clonedOntology != null) {
            manager.removeOntology(clonedOntology);
        }
        timer.stop();
        LOGGER.info("{}, {}, remained Bugs: {}, ActiveAnalysis: {}", timer, log.toString(), remainedCounter.decrementAndGet(), activeAnalysisCounter.decrementAndGet());
        return bug;
    }

    private Set<OWLAxiom> getAxiomsRelatedToUnsatClass(OWLOntology ont, OWLEntity cla) {
        if (Configs.getInstance().isUSE_MODULAR_ONTOLOGY_IN_BUG_FINDER()) {
            return getModuleAxioms(ont, cla);
        } else {
            return ont.getAxioms();
        }
    }

    private Set<OWLAxiom> getModuleAxioms(OWLOntology ontology, OWLEntity entity) {
        Set<OWLAxiom> axioms = new HashSet<>();
        int oldSize = 0;
        if (entity instanceof OWLClass) {
            axioms.addAll(ontology.getAxioms((OWLClass) entity, Imports.INCLUDED));
        } else if (entity instanceof OWLObjectPropertyExpression) {
            axioms.addAll(ontology.getAxioms((OWLObjectPropertyExpression) entity, Imports.INCLUDED));
        }
        Set<OWLEntity> topBottom = getTopBottomEntities();
        while (axioms.size() != oldSize) {
            oldSize = axioms.size();
            Set<OWLEntity> entities = new HashSet<>();
            axioms.stream().map(OWLAxiom::getSignature).forEach(entities::addAll);
            ontology.getAxioms().stream().filter(axiom -> {
                final Set<OWLEntity> signature = axiom.getSignature();
                signature.removeAll(topBottom);
                signature.retainAll(entities);
                return !signature.isEmpty();
            }).forEach(axioms::add);
        }
        LOGGER.debug("Modular Ontology has {} axioms which are {}% of whole ontology", axioms.size(), axioms.size() * 100 / ontology.getAxiomCount());
        return axioms;
    }

    private Set<OWLEntity> getTopBottomEntities() {
        Set<OWLEntity> topBottom = new HashSet<>();
        final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
        topBottom.add(dataFactory.getOWLThing());
        topBottom.add(dataFactory.getOWLNothing());

        topBottom.add(dataFactory.getTopDatatype());
        topBottom.add(dataFactory.getOWLTopDataProperty());
        topBottom.add(dataFactory.getOWLBottomDataProperty());
        topBottom.add(dataFactory.getOWLTopObjectProperty());
        topBottom.add(dataFactory.getOWLBottomObjectProperty());
        return topBottom;
    }

    /**
     * Find MUPS sets causing incoherency of the given class within given ontology
     *
     * @param ont    ontology to be inspected
     * @param entity incoherent class
     * @param log
     * @return Bug associated with the incoherent class
     */
    protected abstract Bug findBug(OWLOntology ont, OWLEntity entity, Set<MUPS> mups, PerformanceLog log);

    protected MUPS getMUPS(OWLOntology ont, OWLEntity entity, Set<MUPS> allMUPSs, PerformanceLog log) {
        final MUPS cachedMUPS = getMUPSFromCache(ont, allMUPSs);
        if (cachedMUPS != null) return cachedMUPS;

        final MUPS mups = findRandomMUPS(ont, entity, allMUPSs, log);
        LOGGER.debug("New MUPS found for Entity:{}, already found: {}", entity, allMUPSs.size());
        LOGGER.debug("{}", Renderer.render(mups));

        return mups;
    }

    protected MUPS findRandomMUPS(OWLOntology ont, OWLEntity entity, Set<MUPS> allMUPSs, PerformanceLog log) {
        final Set<OWLAxiom> axioms = mupsFinder.findMUPS(ont, entity, allMUPSs, log);
        if (axioms == null || axioms.isEmpty())
            return null;
        else {
            return MUPS.build(entity, axioms, profile, ontology);
        }
    }

    protected MUPS getMUPSFromCache(OWLOntology ont, Set<MUPS> allMUPSs) {
        synchronized (allMUPSs) {
            final Optional<MUPS> cachedMUPS =
                    allMUPSs.stream().filter(mups -> mups.stream().allMatch(ont::containsAxiom)).findAny();
            if (cachedMUPS.isPresent()) {
                LOGGER.trace("Using cached Ontology in getMUPS method");
                return cachedMUPS.get();
            }
        }
        return null;
    }
}
