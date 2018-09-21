package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/29/15
 */
public class InformationContentAxiomRanker extends AxiomRanker {
    private static final Logger LOGGER = LogManager.getLogger(InformationContentAxiomRanker.class);

    private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private final Map<OWLEntity, Double> icMap = new ConcurrentHashMap<>();
    private long maxLeaves = 0;

    public InformationContentAxiomRanker() {
    }

    @Override
    protected void init(OWLOntology ont, BugList bugs) {
        super.init(ont, bugs);
        icMap.clear();
        try {
            ontology = manager.createOntology(ont.getAxioms());
            reasoner = ReasonerFactory.getReasoner(ontology);
            maxLeaves = ontology.getSignature().stream().filter(entity -> entity instanceof OWLClass)
                    .filter(entity -> isLeaf((OWLClass) entity)).count();
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }

    }

    @Override
    protected void fini() {
        if (ontology != null) {
            manager.removeOntology(ontology);
        }
    }

    private double getEntityIC(OWLEntity entity) {
        return getEntityIC(entity, false);
    }

    private double getEntityIC(OWLEntity entity, boolean useCachedValue) {
        Double ic = null;
        if (useCachedValue) {
            ic = icMap.get(entity);
        }
        if (ic == null) {

            if (entity instanceof OWLClass) {
                ic = getClassIC((OWLClass) entity);
            } else {
                ic = 0.0;
            }
            if (useCachedValue) {
                icMap.put(entity, ic);
            }
        }
        return ic;

    }

    private boolean isLeaf(OWLClass owlClass) {
        return reasoner.getSubClasses(owlClass, true).isEmpty();
    }

    private Set<OWLClass> getLeaves(OWLClass owlClass) {
        return reasoner.getSubClasses(owlClass, false).getFlattened().stream()
                .filter(this::isLeaf).collect(Collectors.toSet());
    }

    private Set<OWLClass> getSubsumers(OWLClass owlClass) {
        final Set<OWLClass> supClasses = reasoner.getSuperClasses(owlClass, false).getFlattened();
        supClasses.add(owlClass);
        return supClasses;
    }

    private double getClassIC(OWLClass owlClass) {
        return -1 * Math.log(((getLeaves(owlClass).size() / (double) getSubsumers(owlClass).size()) + 1) / (maxLeaves + 1));
    }

    @Override
    protected double getAxiomCost(OWLAxiom axiom) {

        double beforeIC = axiom.getSignature().stream().mapToDouble(entity -> getEntityIC(entity, true)).sum();

        manager.removeAxiom(ontology, axiom);
        reasoner.flush();
        double afterIC = axiom.getSignature().stream().mapToDouble(this::getEntityIC).sum();

        manager.addAxiom(ontology, axiom);
        reasoner.flush();

        return beforeIC - afterIC;
    }
}
