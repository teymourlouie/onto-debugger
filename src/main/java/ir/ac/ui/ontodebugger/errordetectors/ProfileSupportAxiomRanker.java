package ir.ac.ui.ontodebugger.errordetectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/29/15
 */
public class ProfileSupportAxiomRanker extends AxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(ProfileSupportAxiomRanker.class);
    public static final int NEUTRAL_AXIOM_COST = 0;
    protected final double entailedAxiomCost;
    private final ProfileSupportChecker checker;

    /**
     * @param checker
     * @param entailedAxiomCost
     */
    public ProfileSupportAxiomRanker(ProfileSupportChecker checker, double entailedAxiomCost) {
        this.checker = checker;
        this.entailedAxiomCost = entailedAxiomCost;

    }

    @Override
    protected void fini() {
        super.fini();
    }

    @Override
    protected double getAxiomCost(OWLAxiom axiom) {
        return getAxiomSupport(axiom);
    }

    public double getAxiomSupport(OWLAxiom axiom) {
        final double sum = checker.getAxiomSupportStatus(axiom).stream().mapToDouble(status -> getAxiomSupport(axiom, status)).sum();
        if (Double.doubleToRawLongBits(sum) != 0) {
            return sum;
        } else {
            return getNeutralAxiomCost(axiom);
        }
    }

    private synchronized double getAxiomSupport(OWLAxiom axiom, SupportStatus status) {
        switch (status) {
            case AXIOM_ENTAILED:
                return entailedAxiomCost;
            case NEGATION_ENTAILED:
                return -1 * entailedAxiomCost;
            case UNKNOWN:
            default:
                return NEUTRAL_AXIOM_COST;
        }
    }

    protected double getNeutralAxiomCost(OWLAxiom axiom) {
        return NEUTRAL_AXIOM_COST;
    }

    @Override
    public String toString() {
        return "ProfileSupportAxiomRanker";
    }
}
