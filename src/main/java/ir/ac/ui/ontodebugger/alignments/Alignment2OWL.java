package ir.ac.ui.ontodebugger.alignments;

import fr.inrialpes.exmo.align.impl.rel.*;
import ir.ac.ui.ontodebugger.Configs;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/19/15
 */
public class Alignment2OWL {
    private static final Logger LOGGER = LogManager.getLogger(Alignment2OWL.class);

    @Getter
    private final Map<OWLAxiom, Cell> axiomCellMap = new HashMap<>();
    private final Alignment alignment;
    private final EntityResolver resolver;
    public static final IRI MAPPING_IRI = IRI.create(Configs.IRI_PREFIX, "/aligned/");
    private final OWLDataFactory dataFactory;
    private static final IRI IRI_MAPPING_CONFIDENCE = IRI.create(MAPPING_IRI + "#confidence");

    public Alignment2OWL(@Nonnull Alignment alignment, @Nonnull EntityResolver resolver, double threshold) {
        this.alignment = alignment;
        this.resolver = resolver;
        dataFactory = OWLManager.getOWLDataFactory();

        for (Cell cell : alignment) {
            if (cell.getStrength() >= threshold) {
                final OWLAxiom axiom = getAxiom(cell);
                if (axiom != null) {
                    axiomCellMap.put(axiom, cell);
                }
            }
        }
    }

    private OWLAxiom getAxiom(Cell cell) {
        OWLAxiom axiom = null;
        try {
            OWLEntity ob1 = resolver.getEntity(cell.getObject1AsURI(this.alignment));
            OWLEntity ob2 = resolver.getEntity(cell.getObject2AsURI(this.alignment));
            if (!isValidPair(ob1, ob2)) {
                return axiom;
            }
            final Relation rel = cell.getRelation();
            if (rel instanceof EquivRelation) {
                if (ob1 instanceof OWLClassExpression) {
                    axiom = dataFactory.getOWLEquivalentClassesAxiom((OWLClassExpression) ob1, (OWLClassExpression) ob2);
                } else if (ob1 instanceof OWLDataProperty) {
                    axiom = dataFactory.getOWLEquivalentDataPropertiesAxiom((OWLDataProperty) ob1, (OWLDataProperty) ob2);
                } else if (ob1 instanceof OWLObjectProperty) {
                    axiom = dataFactory.getOWLEquivalentObjectPropertiesAxiom((OWLObjectProperty) ob1, (OWLObjectProperty) ob2);
                } else if (ob1 instanceof OWLIndividual) {
                    axiom = dataFactory.getOWLSameIndividualAxiom((OWLIndividual) ob1, (OWLIndividual) ob2);
                }
            } else if (rel instanceof IncompatRelation) {
                if (ob1 instanceof OWLClassExpression) {
                    axiom = dataFactory.getOWLDisjointClassesAxiom((OWLClassExpression) ob1, (OWLClassExpression) ob2);
                } else if (ob1 instanceof OWLDataProperty) {
                    axiom = dataFactory.getOWLDisjointDataPropertiesAxiom((OWLDataProperty) ob1, (OWLDataProperty) ob2);
                } else if (ob1 instanceof OWLObjectProperty) {
                    axiom = dataFactory.getOWLDisjointObjectPropertiesAxiom((OWLObjectProperty) ob1, (OWLObjectProperty) ob2);
                } else if (ob1 instanceof OWLIndividual) {
                    axiom = dataFactory.getOWLDifferentIndividualsAxiom((OWLIndividual) ob1, (OWLIndividual) ob2);
                }
            } else if (rel instanceof SubsumeRelation) {
                if (ob1 instanceof OWLClassExpression) {
                    axiom = dataFactory.getOWLSubClassOfAxiom((OWLClassExpression) ob1, (OWLClassExpression) ob2);
                } else if (ob1 instanceof OWLDataProperty) {
                    axiom = dataFactory.getOWLSubDataPropertyOfAxiom((OWLDataProperty) ob1, (OWLDataProperty) ob2);
                } else if (ob1 instanceof OWLObjectProperty) {
                    axiom = dataFactory.getOWLSubObjectPropertyOfAxiom((OWLObjectProperty) ob1, (OWLObjectProperty) ob2);
                }
            } else if (rel instanceof SubsumedRelation) {
                if (ob1 instanceof OWLClassExpression) {
                    axiom = dataFactory.getOWLSubClassOfAxiom((OWLClassExpression) ob2, (OWLClassExpression) ob1);
                } else if (ob1 instanceof OWLDataProperty) {
                    axiom = dataFactory.getOWLSubDataPropertyOfAxiom((OWLDataProperty) ob2, (OWLDataProperty) ob1);
                } else if (ob1 instanceof OWLObjectProperty) {
                    axiom = dataFactory.getOWLSubObjectPropertyOfAxiom((OWLObjectProperty) ob2, (OWLObjectProperty) ob1);
                }
            } else if (rel instanceof InstanceOfRelation) {
                if (ob1 instanceof OWLIndividual) {
                    axiom = dataFactory.getOWLClassAssertionAxiom((OWLClassExpression) ob2, (OWLIndividual) ob1);
                }
            } else if (rel instanceof HasInstanceRelation) {
                if (ob2 instanceof OWLIndividual) {
                    axiom = dataFactory.getOWLClassAssertionAxiom((OWLClassExpression) ob1, (OWLIndividual) ob2);
                }
            }

            if (axiom != null) {
                OWLAnnotationProperty annProperty = dataFactory.getOWLAnnotationProperty(IRI_MAPPING_CONFIDENCE);
                OWLLiteral confidenceLiteral = dataFactory.getOWLLiteral(cell.getStrength());
                OWLAnnotation annConfidence = dataFactory.getOWLAnnotation(annProperty, confidenceLiteral);
                final Set<OWLAnnotation> annotations = new HashSet<>();
                annotations.add(annConfidence);
                axiom = axiom.getAnnotatedAxiom(annotations);
            }
        } catch (AlignmentException e) {
            LOGGER.catching(e);
        }
        return axiom;
    }

    private boolean isValidPair(OWLEntity ob1, OWLEntity ob2) {
        boolean valid = false;
        if (ob1 != null && ob2 != null &&
                ((ob1 instanceof OWLClass && ob2 instanceof OWLClass) ||
                        (ob1 instanceof OWLDataProperty && ob2 instanceof OWLDataProperty) ||
                        (ob1 instanceof OWLObjectProperty && ob2 instanceof OWLObjectProperty) ||
                        (ob1 instanceof OWLIndividual && ob2 instanceof OWLIndividual))) {
            valid = true;
        }
        return valid;
    }

    public OWLOntology getOWLOntology() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            return manager.createOntology(axiomCellMap.keySet());
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
            return null;
        }
    }

}
