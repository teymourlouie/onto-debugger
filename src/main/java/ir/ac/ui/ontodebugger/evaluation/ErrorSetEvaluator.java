package ir.ac.ui.ontodebugger.evaluation;

import lombok.Getter;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/22/15
 */
public abstract class ErrorSetEvaluator {
    @Getter
    protected Set<OWLAxiom> errorAxioms;
    @Getter
    protected Set<OWLAxiom> trueAxioms;
    @Getter
    protected Set<OWLAxiom> falseAxioms;

    public Set<OWLAxiom> getNotDetectedAxioms() {
        return errorAxioms.stream().filter(axiom -> trueAxioms.stream().noneMatch(tAxiom -> tAxiom.getAxiomWithoutAnnotations().equals(axiom.getAxiomWithoutAnnotations()))).collect(Collectors.toSet());
    }

    public abstract Evaluation evaluate(OWLOntology ontology, Set<OWLAxiom> errorSet);
}
