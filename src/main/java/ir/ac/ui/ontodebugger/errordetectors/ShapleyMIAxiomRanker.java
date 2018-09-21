package ir.ac.ui.ontodebugger.errordetectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/29/15
 */
public class ShapleyMIAxiomRanker extends AxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(ShapleyMIAxiomRanker.class);

    /**
     */
    public ShapleyMIAxiomRanker() {
    }

    @Override
    protected double getAxiomCost(OWLAxiom axiom) {
        final double v = bugs.getMUPSContainsAxiom(axiom).stream().mapToDouble(m -> 1.0 / m.size()).sum();
        return 1 / v;
    }
}
