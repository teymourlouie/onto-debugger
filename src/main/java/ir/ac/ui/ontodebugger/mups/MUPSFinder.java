package ir.ac.ui.ontodebugger.mups;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/24/16.
 */
public interface MUPSFinder {

    Set<OWLAxiom> findMUPS(OWLOntology ont, OWLEntity entity, Set<MUPS> allMUPSs, PerformanceLog log);
}
