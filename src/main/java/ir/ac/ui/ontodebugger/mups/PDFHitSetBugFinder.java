package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.Bug;
import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/24/16.
 */
public class PDFHitSetBugFinder extends DFHitSetBugFinder {
    private static final Logger LOGGER = LogManager.getLogger(PDFHitSetBugFinder.class);

    public PDFHitSetBugFinder(KnowledgeBaseProfile profile, OWLOntology ontology) {
        super(profile, ontology);
    }

    @Override
    protected Bug findBug(OWLOntology ont, OWLEntity entity, Set<MUPS> cache, PerformanceLog log) {
        LOGGER.debug("Analysing unsatisfiable Class:{} started...", entity);
        Set<MUPS> allMUPSs = new HashSet<>(cache);
        final Set<Set<OWLAxiom>> satisfiablePaths = new HashSet<>();

        AtomicInteger earlyTerminationCounter = new AtomicInteger(0);
        AtomicInteger nodeCounter = new AtomicInteger(0);

        final MUPS newMUPS = getMUPS(ont, entity, allMUPSs, log);
        if (newMUPS != null) {
            allMUPSs.add(newMUPS);
            hitSetFindMups(ont, entity, allMUPSs, newMUPS, new HashSet<>(), satisfiablePaths, nodeCounter, earlyTerminationCounter, log);

            LOGGER.info("Hitset Tree for {} has Node: {}, SatisfiablePath: {}, EarlyTermination: {} ",
                    entity, nodeCounter.get(), satisfiablePaths.size(), earlyTerminationCounter.get());
            return new Bug(entity, allMUPSs, satisfiablePaths);
        } else
            return null;
    }

    protected void hitSetFindMups(@Nonnull OWLOntology ont,
                                  @Nonnull OWLEntity entity,
                                  @Nonnull Set<MUPS> allMUPSs,
                                  @Nonnull MUPS currentMUPS,
                                  @Nonnull Set<OWLAxiom> currentPath,
                                  @Nonnull Set<Set<OWLAxiom>> satisfiablePaths,
                                  @Nonnull AtomicInteger nodeCounter,
                                  @Nonnull AtomicInteger earlyTerminationCounter, PerformanceLog log) {
        LOGGER.trace("HitSet Algorithm for class:{}, allMUPSs:{}, Diagnoses:{}, CurrentPath:{}",
                entity, allMUPSs.size(), satisfiablePaths.size(), currentPath.size());
        nodeCounter.incrementAndGet();

        currentMUPS.parallelStream().forEach(axiom -> {
            Set<OWLAxiom> path = new HashSet<>(currentPath);
            path.add(axiom);

            final boolean earlyTermination;
            synchronized (satisfiablePaths) {
                earlyTermination = satisfiablePaths.stream().anyMatch(path::containsAll);
            }
            if (earlyTermination) {
                LOGGER.trace("Early Path Termination");
                earlyTerminationCounter.incrementAndGet();
                return;
            }

            // this path is not a superset for any other satisfiablePath
            final OWLOntologyManager m = OWLManager.createOWLOntologyManager();

            try {
                OWLOntology temp = m.createOntology(ont.getAxioms());
                m.removeAxiom(temp, axiom);

                final OWLReasoner reasoner = ReasonerFactory.getReasoner(temp);

                MUPS mups = null;
                if (!OntologyHelper.isSatisfiable(reasoner, entity)) {
                    mups = getMUPS(temp, entity, allMUPSs, log);
                }


                if (mups == null) {
                    LOGGER.debug("New Diagnosis(Satisfiable path) found for class: {}", entity);
                    // add to list of satisfiable paths
                    synchronized (satisfiablePaths) {
                        satisfiablePaths.add(new HashSet<>(path));
                    }
                } else {
                    if (!allMUPSs.contains(mups)) {
                        allMUPSs.add(mups);
                    }
                    // Recursively build the HitSet Tree
                    hitSetFindMups(temp, entity, allMUPSs, mups, path, satisfiablePaths, nodeCounter, earlyTerminationCounter, log);
                }
                m.removeOntology(temp);
            } catch (OWLOntologyCreationException e) {
                LOGGER.catching(e);
            }
        });
    }
}
