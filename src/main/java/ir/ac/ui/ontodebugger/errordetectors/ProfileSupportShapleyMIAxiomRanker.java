package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/29/15
 */
public class ProfileSupportShapleyMIAxiomRanker extends ProfileSupportAxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(ProfileSupportShapleyMIAxiomRanker.class);
    private final ShapleyMIAxiomRanker shapleyMI;

    public ProfileSupportShapleyMIAxiomRanker(ProfileSupportChecker checker, double entailedAxiomCost) {
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
        return shapleyMI.getAxiomCost(axiom);
    }

    @Override
    public String toString() {
        return "ProfileSupport+ShapleyMIAxiomRanker";
    }
}
