package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.Renderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/29/15
 */
public class SwoopAxiomRanker extends AxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(SwoopAxiomRanker.class);

    private OWLReasoner reasoner;

    private double maxImpact = Double.MAX_VALUE;

    private double[] weights = new double[3];

    private static final int FREQ = 0;
    private static final int IMPACT = 1;
    private static final int USAGE = 2;
    private OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

    /**
     */
    public SwoopAxiomRanker() {
        setWeights(0.9, 0.7, 0.1);
    }

    /**
     * get semantic impact value of the axiom set
     *
     * @param axiomSet
     * @return number of axioms in the set
     */
    private int getImpactValue(Set<OWLAxiom> axiomSet) {
        return axiomSet.size();
    }

    /*
     * set weights of parameters for computing axiom ranks
     */
    private void setWeights(double freq, double impact, double usage) {
        weights[FREQ] = freq;
        weights[IMPACT] = impact;
        weights[USAGE] = usage;
    }

    @Override
    public void init(OWLOntology ont, BugList bugs) {
        final OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();
        OWLOntology ontology = ont;
        try {
            ontology = manager.createOntology(ont.getAxioms());
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }
        super.init(ontology, bugs);

        reasoner = ReasonerFactory.getReasoner(ontology);
    }

    @Override
    protected double getAxiomCost(OWLAxiom axiom) {
        double cost;
        final double freq = calculateFreq(axiom);
        final double impact = calculateImpact(axiom);
        final double usage = calculateUsage(axiom);

        cost = weights[FREQ] / freq;
        cost += weights[IMPACT] * impact;
        cost += weights[USAGE] * usage;

        LOGGER.debug("{} FREQ={}, IMPACT={}, USAGE={}", Renderer.render(axiom), freq, impact, usage);
        return cost;
    }

    /**
     * get frequency of axiom in MUPS sets
     *
     * @param axiom
     * @return number of MUPS containing the axiom
     */
    private double calculateFreq(OWLAxiom axiom) {
        return bugs.getMUPSContainsAxiom(axiom).size();
    }

    /**
     * get normalized impact value of the axiom
     *
     * @param axiom
     * @return
     */
    private double calculateImpact(OWLAxiom axiom) {
        return getImpactValue(getSemanticImpact(axiom)) / maxImpact;
    }

    /**
     * calculate usage of signature of the axiom by counting number of axioms which they are included in them.
     *
     * @param axiom
     * @return number axioms having some of the given axiom's signature in their signature
     */
    private double calculateUsage(OWLAxiom axiom) {
        Set<OWLAxiom> usage = new HashSet<>();
        axiom.getSignature().stream().forEach(owlEntity -> usage.addAll(EntitySearcher.getReferencingAxioms(owlEntity, ont)));
        return usage.size() / (double) ont.getAxiomCount();
    }

    /**
     * Get entailments impacted when removing the given axiom from ontology
     *
     * @param axiom to be evaluated
     * @return Set of entailments
     */
    public Set<OWLAxiom> getSemanticImpact(OWLAxiom axiom) {
        final Set<OWLAxiom> entailments = new HashSet<>();
        if (axiom instanceof OWLSubClassOfAxiom) {
            entailments.addAll(getSubClassOfEntailments((OWLSubClassOfAxiom) axiom));
        } else if (axiom instanceof OWLEquivalentClassesAxiom) {
            final Set<OWLClassExpression> classSet = ((OWLEquivalentClassesAxiom) axiom).getClassExpressions();
            classSet.stream().forEach(c1 -> classSet.stream().filter(c2 -> !c2.equals(c1))
                    .forEach(c2 -> entailments.addAll(getSubClassOfEntailments(dataFactory.getOWLSubClassOfAxiom(c1, c2)))));
        } else if (axiom instanceof OWLDisjointClassesAxiom) {
            entailments.addAll(getDisjointEntailments((OWLDisjointClassesAxiom) axiom));
        } else if (axiom instanceof OWLPropertyDomainAxiom) {
            entailments.addAll(getPropertyDomainEntailments((OWLPropertyDomainAxiom) axiom));
        } else if (axiom instanceof OWLObjectPropertyRangeAxiom) {
            entailments.addAll(getPropertyRangeEntailments((OWLObjectPropertyRangeAxiom) axiom));
        }
        return entailments;
    }

    /**
     * get entailments based on given property domain axiom
     *
     * @param pd property domain axiom
     * @return entailments like superClasses of original domain are also a domain for the property
     */
    private Set<OWLAxiom> getPropertyDomainEntailments(OWLPropertyDomainAxiom pd) {
        Set<OWLAxiom> entailments = new HashSet<>();
        final OWLClassExpression domain = pd.getDomain();
        reasoner.getSuperClasses(domain, false).getFlattened().stream().filter(supClass -> isValidClass(supClass))
                .forEach(supClass -> {
                    OWLAxiom ax = null;
                    if (pd instanceof OWLDataPropertyDomainAxiom) {
                        ax = dataFactory.getOWLDataPropertyDomainAxiom(((OWLDataPropertyDomainAxiom) pd).getProperty(), supClass);
                    } else if (pd instanceof OWLObjectPropertyDomainAxiom) {
                        ax = dataFactory.getOWLObjectPropertyDomainAxiom(((OWLObjectPropertyDomainAxiom) pd).getProperty(), supClass);
                    } else {
                        LOGGER.error("OWLPropertyDomainAxiom is not OWLDataPropertyDomainAxiom nor OWLObjectPropertyDomainAxiom");
                        LOGGER.error("{}", Renderer.render(pd));
                    }
                    if (ax != null) {
                        entailments.add(ax);
                    }
                });
        return entailments;
    }

    /**
     * get entailments based on given property range axiom
     *
     * @param pr property range axiom
     * @return entailments like superClasses of original range are also a range for the property
     */
    private Set<OWLAxiom> getPropertyRangeEntailments(OWLObjectPropertyRangeAxiom pr) {
        Set<OWLAxiom> entailments = new HashSet<>();
        final OWLClassExpression range = pr.getRange();
        reasoner.getSuperClasses(range, false).getFlattened().stream().filter(this::isValidClass).forEach(supClass -> {
            OWLAxiom ax = dataFactory.getOWLObjectPropertyRangeAxiom(pr.getProperty(), supClass);
            entailments.add(ax);
        });
        return entailments;
    }

    /**
     * get entailments based on given subclass axiom
     *
     * @param subAx
     * @return set of entailments like subclasses of subclass are also subclass of superclasses of the superclass of the axiom
     */
    private Set<OWLAxiom> getSubClassOfEntailments(OWLSubClassOfAxiom subAx) {
        Set<OWLAxiom> entailments = new HashSet<>();
        if (subAx.getSubClass() instanceof OWLClass && subAx.getSuperClass() instanceof OWLClass) {
            // add disjoint entailment based on the relation between subclass and superclass
            entailments.addAll(getDisjointBySubsumptionEntailments(subAx.getSubClass(), subAx.getSuperClass()));

            reasoner.getSubClasses(subAx.getSubClass(), false).getFlattened()
                    .stream().filter(this::isValidClass)
                    .forEach(subClass -> reasoner.getSuperClasses(subAx.getSuperClass(), false).getFlattened()
                            .stream().filter(this::isValidClass).forEach(supClass -> {
                                OWLAxiom ax = dataFactory.getOWLSubClassOfAxiom(subClass, supClass);
                                entailments.add(ax);

                                // add disjoint entailment based on the relation between subclass and superclass
                                entailments.addAll(getDisjointBySubsumptionEntailments(subClass, supClass));
                            }));
        }
        return entailments;
    }

    /**
     * get disjoint entailments based on relation between subclass and supClass
     *
     * @param subClass
     * @param supClass
     * @return set of entailments like subclass is also disjoint with superclasses of the superclass
     */
    private Set<OWLAxiom> getDisjointBySubsumptionEntailments(OWLClassExpression subClass, OWLClassExpression supClass) {
        Set<OWLClassExpression> disSet = new HashSet<>();
        disSet.add(subClass);
        disSet.addAll(reasoner.getDisjointClasses(supClass).getFlattened());
        return getDisjointEntailments(dataFactory.getOWLDisjointClassesAxiom(disSet));
    }

    /**
     * get entailments based on disjoint classes in disjointness axiom
     *
     * @param axiom
     * @return set of entailments like subclasses of disjoint classes are also disjoint with each other
     */
    private Set<OWLAxiom> getDisjointEntailments(OWLDisjointClassesAxiom axiom) {
        Set<OWLAxiom> entailments = new HashSet<>();

        final Set<OWLClassExpression> disjoints = axiom.getClassExpressions();

        new ArrayList<>(disjoints).stream().filter(classExpression -> !classExpression.equals(dataFactory.getOWLNothing())).forEach(classExpression -> {
            //remove
            disjoints.remove(classExpression);

            Set<OWLClass> subClasses = reasoner.getSubClasses(classExpression, false).getFlattened();
            subClasses.removeAll(reasoner.getEquivalentClasses(dataFactory.getOWLNothing()).getEntities());
            subClasses.stream().filter(subClass -> isValidClass(subClass))
                    .forEach(subClass -> disjoints.stream().filter(disClassExpr -> !disClassExpr.equals(subClass))
                            .forEach(disClassExpr -> {
                                Set<OWLClassExpression> newDis = new HashSet<>();
                                newDis.add(subClass);
                                newDis.add(disClassExpr);
                                OWLDisjointClassesAxiom ax = dataFactory.getOWLDisjointClassesAxiom(newDis);
                                entailments.add(ax);
                            }));
            //add again
            disjoints.add(classExpression);
        });
        return entailments;
    }

    /**
     * check validity of class
     *
     * @param owlClass
     * @return true if owlClass is not OWLNothing and is coherent
     */
    private boolean isValidClass(OWLClass owlClass) {
        return !owlClass.equals(dataFactory.getOWLNothing())
                && !owlClass.equals(dataFactory.getOWLThing())
                && !reasoner.getEquivalentClasses(owlClass).contains(dataFactory.getOWLNothing());
    }
}
