package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Created by mehdi on 3/2/16.
 */
public class ShapleySupportAxiomRanker extends ProfileSupportAxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(ShapleySupportAxiomRanker.class);

    private final ShapleyMIAxiomRanker shapleyMI;

    /**
     * @param checker
     * @param entailedAxiomCost
     */
    public ShapleySupportAxiomRanker(ProfileSupportChecker checker, double entailedAxiomCost) {
        super(checker, entailedAxiomCost);
        shapleyMI = new ShapleyMIAxiomRanker();
    }

    @Override
    protected void init(OWLOntology ont, BugList bugs) {
        super.init(ont, bugs);
        shapleyMI.init(ont, bugs);
    }

    @Override
    protected void fini() {
        super.fini();
        shapleyMI.fini();
    }

    @Override
    protected double getNeutralAxiomCost(OWLAxiom axiom) {
        return 1;
    }

    @Override
    protected double getAxiomCost(OWLAxiom axiom) {
        final double support = super.getAxiomCost(axiom);
        final double shapley = shapleyMI.getAxiomCost(axiom);
        if (support >= 0) {
            return support * shapley;
        } else {
            return support / shapley;
        }
    }

    @Override
    public String toString() {
        return "ShapleySupportMIAxiomRanker";
    }
}
