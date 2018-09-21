package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.Bug;
import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static ir.ac.ui.ontodebugger.util.OntologyHelper.isSatisfiable;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/9/15
 */
public class DFHitSetBugFinder extends BugFinder {
    private static final Logger LOGGER = LogManager.getLogger(DFHitSetBugFinder.class);

    private final Map<MUPS, Integer> mupsNo = new ConcurrentHashMap<>();
    private final Map<OWLAxiom, Integer> axiomNo = new ConcurrentHashMap<>();

    public DFHitSetBugFinder(KnowledgeBaseProfile profile, OWLOntology ontology) {
        super(profile, ontology);
    }


    @Override
    protected Bug findBug(OWLOntology ont, OWLEntity entity, Set<MUPS> cache, PerformanceLog log) {
        LOGGER.debug("Analysing unsatisfiable Class:{} started...", entity);
        Set<MUPS> allMUPSs = new HashSet<>(cache);
        final Set<Set<OWLAxiom>> satisfiablePaths = new HashSet<>();

        AtomicInteger earlyTerminationCounter = new AtomicInteger(0);
        AtomicInteger nodeCounter = new AtomicInteger(0);

        final OWLReasoner reasoner = ReasonerFactory.getReasoner(ont);

        final MUPS newMUPS;
        newMUPS = getMUPS(ont, entity, allMUPSs, log);
        if (newMUPS != null) {
            allMUPSs.add(newMUPS);
            hitSetFindMups(ont, entity, allMUPSs, newMUPS, new HashSet<>(), satisfiablePaths, reasoner, nodeCounter, earlyTerminationCounter, log);

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
                                  @Nonnull OWLReasoner reasoner,
                                  @Nonnull AtomicInteger nodeCounter,
                                  @Nonnull AtomicInteger earlyTerminationCounter, PerformanceLog log) {
        LOGGER.trace("HitSet Algorithm for class:{}, allMUPSs:{}, Diagnoses:{}, CurrentPath:{}",
                entity, allMUPSs.size(), satisfiablePaths.size(), currentPath.size());
        LOGGER.trace("Expanding MUPS {}", mupsNo.get(currentMUPS));
        nodeCounter.incrementAndGet();

        for (OWLAxiom axiom : currentMUPS) {
            currentPath.add(axiom);
            LOGGER.trace("Removing axiom {}", axiomNo.get(axiom));

            if (satisfiablePaths.stream().anyMatch(currentPath::containsAll)) {
                LOGGER.debug("Early Path Termination");
                earlyTerminationCounter.incrementAndGet();
            } else { // this path is not a superset for any other satisfiablePath
                ont.getOWLOntologyManager().removeAxiom(ont, axiom);

                MUPS mups = null;
                if (!isSatisfiable(reasoner, entity)) {
                    mups = getMUPS(ont, entity, allMUPSs, log);
                }

                if (mups == null) {
                    LOGGER.debug("New Diagnosis(Satisfiable path) found for class: {}", entity);
                    // add to list of satisfiable paths
                    satisfiablePaths.add(new HashSet<>(currentPath));
                } else {
                    if (!allMUPSs.contains(mups)) {
                        allMUPSs.add(mups);
                    }
                    // Recursively build the HitSet Tree
                    hitSetFindMups(ont, entity, allMUPSs, mups, currentPath, satisfiablePaths, reasoner, nodeCounter, earlyTerminationCounter, log);
                }
                ont.getOWLOntologyManager().addAxiom(ont, axiom);
            }
            currentPath.remove(axiom);
        }
        LOGGER.trace("Expanding MUPS {} finished!", mupsNo.get(currentMUPS));

    }


}
