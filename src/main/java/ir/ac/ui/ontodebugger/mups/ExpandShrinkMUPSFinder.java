package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collections;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/24/16.
 */
public abstract class ExpandShrinkMUPSFinder implements MUPSFinder {
    private static final Logger LOGGER = LogManager.getLogger(ExpandShrinkMUPSFinder.class);

    @Override
    public Set<OWLAxiom> findMUPS(OWLOntology ont, OWLEntity entity, Set<MUPS> allMUPSs, PerformanceLog log) {
        Set<OWLAxiom> mups = Collections.emptySet();

        final OWLOntologyManager m = OWLManager.createOWLOntologyManager();

        OWLOntology temp;
        try {
            temp = m.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        OWLReasoner reasoner = ReasonerFactory.getReasoner(temp);
        Timer tempTimer = Timer.start("expand");
        boolean found = expand(ont, entity, temp, reasoner, allMUPSs);
        tempTimer.stop();
        log.getExpandTime().addAndGet(tempTimer.getElapsedTimeMillis());


        if (found) { // go to prune it
            LOGGER.debug("Pruning started with {} axioms ({}%)", temp.getAxiomCount(), temp.getAxiomCount() * 100 / ont.getAxiomCount());
            tempTimer = Timer.start("shrink");
            LOGGER.debug("{} start shrink", entity);
            mups = shrink(entity, temp, reasoner, allMUPSs);
            LOGGER.debug("{} finish shrink", entity);
            tempTimer.stop();
            log.getShrinkTime().addAndGet(tempTimer.getElapsedTimeMillis());
            LOGGER.debug("Pruning finished with {} axioms", mups.size());
        }
        m.removeOntology(temp);
        return mups;
    }

    /**
     * Expand the ontology temp until clazz becomes unsatisfiable (temp includes an MUPS for clazz)
     *
     * @param ont      ontology to select axioms to find MUPS
     * @param entity   class which should be unsatisfiable in temp
     * @param temp     empty ontology that will include MUPS axioms when the method returns
     * @param reasoner an already initiated reasoner on temp
     * @param allMUPSs
     * @return true if it could find an MUPS for clazz, otherwise returns false
     */
    protected abstract boolean expand(OWLOntology ont, OWLEntity entity, OWLOntology temp, OWLReasoner reasoner, Set<MUPS> allMUPSs);

    /**
     * find an MUPS for clazz inside temp by shrinking it's free axioms
     *
     * @param entity   class that is unsatisfiable in temp
     * @param temp     ontology which includes at least one MUPS for clazz
     * @param reasoner an already initiated reasoner on temp
     * @param allMUPSs
     * @return set of OWLAxioms as an MUPS for clazz
     */
    protected abstract Set<OWLAxiom> shrink(OWLEntity entity, OWLOntology temp, OWLReasoner reasoner, Set<MUPS> allMUPSs);


}
