package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/29/15
 */
public abstract class AxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(AxiomRanker.class);

    protected OWLOntology ont;
    protected BugList bugs;

    public AxiomRanker() {
    }

    protected void init(OWLOntology ont, BugList bugs) {
        this.bugs = bugs;
        this.ont = ont;
    }

    protected void fini() {

    }


    /**
     * get cost of selecting the axiom as an error
     *
     * @param axiom
     * @return cost as a double value
     */
    protected abstract double getAxiomCost(OWLAxiom axiom);

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
