package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.Bug;
import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import ir.ac.ui.ontodebugger.util.Renderer;
import ir.ac.ui.ontodebugger.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/24/16.
 */
public class HitSetBugFinder extends BugFinder {
    private static final Logger LOGGER = LogManager.getLogger(HitSetBugFinder.class);
    private final boolean expandTreeInParallel;
    private final boolean synchronizedFindMups;

    public HitSetBugFinder(KnowledgeBaseProfile profile, OWLOntology ontology, boolean expandTreeInParallel, boolean synchronizedFindMups) {
        super(profile, ontology);
        this.expandTreeInParallel = expandTreeInParallel;
        this.synchronizedFindMups = synchronizedFindMups;
    }

    @Override
    protected Bug findBug(OWLOntology ont, OWLEntity entity, Set<MUPS> cache, PerformanceLog log) {
        LOGGER.debug("Analysing unsatisfiable Entity:{} started...", entity);
        ReentrantLock lock = new ReentrantLock();
        Set<MUPS> allMUPSs = new HashSet<>(cache);
        final Set<Set<OWLAxiom>> satisfiablePaths = new HashSet<>();
        final Set<Set<OWLAxiom>> examinedPaths = new HashSet<>();
        List<TreeNode> notExpandedNodes = new ArrayList<>();

        AtomicInteger earlyTerminationCounter = new AtomicInteger(0);
        AtomicInteger nodeCounter = new AtomicInteger(0);

        MUPS newMUPS;
        final Optional<MUPS> any = cache.stream().findAny();
        if (any.isPresent()) {
            newMUPS = any.get();
        } else {
            Timer tempTimer = Timer.start("findRandomMUPS");
            newMUPS = findRandomMUPS(ont, entity, allMUPSs, log);
            tempTimer.stop();
            log.getFindRandomMUPSTime().addAndGet(tempTimer.getElapsedTimeMillis());
            allMUPSs.add(newMUPS);
        }

        if (newMUPS == null) {
            return null;
        }

        notExpandedNodes.add(new TreeNode(newMUPS, new HashSet<>(), nodeCounter.incrementAndGet()));

        while (!notExpandedNodes.isEmpty()) {
            List<TreeNode> currentWaitingNodes = new ArrayList<>(notExpandedNodes);

            if (expandTreeInParallel) {
                LOGGER.debug("Expanding {} MUPS in parallel for Entity: {}", currentWaitingNodes.size(), entity);
            }

            getMupsPathPairExpanderStream(currentWaitingNodes).forEach(currentNode -> {
                synchronized (notExpandedNodes) {
                    notExpandedNodes.remove(currentNode);
                }

                final MUPS currentMUPS = currentNode.mups;
                final Set<OWLAxiom> currentPath = currentNode.path;
                getMUPSExpanderStream(currentMUPS).forEach(axiom -> {
                    LOGGER.trace("Node<{}> is expanded by removing axiom: {}", currentNode.nodeNumber, axiom);
                    Set<OWLAxiom> path = new HashSet<>(currentPath);
                    path.add(axiom);

                    synchronized (satisfiablePaths) {
                        synchronized (examinedPaths) {
                            if (isEarlyTerminated(path, satisfiablePaths, examinedPaths)) {
                                LOGGER.trace("Path is early terminated!");
                                earlyTerminationCounter.incrementAndGet();
                                return;
                            }
                            // add to the list of examined paths
                            examinedPaths.add(path);
                        }
                    }

                    // this path is not a superset for any other satisfiablePath
                    final OWLOntologyManager m = OWLManager.createOWLOntologyManager();


                    try {
                        OWLOntology temp = m.createOntology(ont.getAxioms());
                        m.removeAxioms(temp, path);

                        MUPS mups = getMUPSFromCache(temp, allMUPSs);
                        boolean useCachedMUPS = false;
                        if (mups == null) {
                            // try acquire lock to call find random MUPS
                            if (synchronizedFindMups) {
                                final boolean locked = lock.tryLock();
                                if (!locked) {
                                    lock.lock();
                                    mups = getMUPSFromCache(temp, allMUPSs);
                                }
                            }

                            // really call find Random MUPS method
                            if (mups == null) {
                                final OWLReasoner reasoner = ReasonerFactory.getReasoner(temp);
                                Timer satTimer = Timer.start("satCheck");
                                final boolean satisfiable = OntologyHelper.isSatisfiable(reasoner, entity);
                                satTimer.stop();
                                log.getSatisfiableChecksTime().addAndGet(satTimer.getElapsedTimeMillis());
                                if (!satisfiable) {
                                    Timer t = Timer.start("findRandomMUPS");
                                    mups = findRandomMUPS(temp, entity, allMUPSs, log);
                                    t.stop();
                                    log.getFindRandomMUPSTime().addAndGet(t.getElapsedTimeMillis());
                                }
                            } else {
                                useCachedMUPS = true;
                            }

                            // release the lock
                            if (synchronizedFindMups) {
                                lock.unlock();
                            }
                        } else {
                            useCachedMUPS = true;
                        }

                        if (mups == null) {
                            // add to list of satisfiable paths
                            synchronized (satisfiablePaths) {
                                satisfiablePaths.add(new HashSet<>(path));
                            }
                            LOGGER.debug("New Diagnosis(Satisfiable path) of size {} found for Entity: {}, Found:{}", path.size(), entity, satisfiablePaths.size());
                        } else {
                            synchronized (allMUPSs) {
                                if (!allMUPSs.contains(mups)) {
                                    allMUPSs.add(mups);
                                } else if (!useCachedMUPS) {
                                    LOGGER.info("Entity {}, MUPS is found in Parallel and is useless!!!!", Renderer.render(entity));
                                }
                            }
                            synchronized (notExpandedNodes) {
                                notExpandedNodes.add(new TreeNode(mups, path, nodeCounter.incrementAndGet()));
                            }
                        }
                        m.removeOntology(temp);
                    } catch (OWLOntologyCreationException e) {
                        LOGGER.catching(e);
                    }
                });
            });
        }
        LOGGER.info("Hitset Tree for Entity: {} has Node: {}, SatisfiablePath: {}, EarlyTermination: {}",
                entity, nodeCounter.get(), satisfiablePaths.size(), earlyTerminationCounter.get());
        return new Bug(entity, allMUPSs, satisfiablePaths);
    }

    protected boolean isEarlyTerminated(Set<OWLAxiom> path, Set<Set<OWLAxiom>> satisfiablePaths, Set<Set<OWLAxiom>> examinedPaths) {
        synchronized (satisfiablePaths) {
            // if this path is a super set of some satisfiable path, then this path is also a satisfiable path
            if (satisfiablePaths.stream().anyMatch(path::containsAll)) {
                LOGGER.trace("Early Path Termination: Path is a super set of a satisfiable path.");
                return true;
            }
        }

        synchronized (examinedPaths) {
            // if path is already examined, then no need to expand this path anymore
            if (examinedPaths.stream().anyMatch(oldPath -> oldPath.equals(path))) {
                LOGGER.trace("Early Path Termination: Path is already examined");
                return true;
            }
        }
        return false;
    }

    private Stream<OWLAxiom> getMUPSExpanderStream(MUPS currentMUPS) {
        if (expandTreeInParallel) {
            return currentMUPS.parallelStream();
        } else {
            return currentMUPS.stream();
        }
    }

    private Stream<TreeNode> getMupsPathPairExpanderStream(List<TreeNode> currentWaitingNodes) {
        if (expandTreeInParallel) {
            return currentWaitingNodes.parallelStream();
        } else {
            return currentWaitingNodes.stream();
        }
    }

    protected class TreeNode {
        public final MUPS mups;
        public final Set<OWLAxiom> path;
        public final int nodeNumber;

        private TreeNode(MUPS mups, Set<OWLAxiom> path, int nodeNumber) {
            this.mups = mups;
            this.path = path;
            this.nodeNumber = nodeNumber;
        }
    }
}
