package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.Configs;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.MultiThreadProcess;
import ir.ac.ui.ontodebugger.util.Renderer;
import ir.ac.ui.ontodebugger.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 11/19/15
 */
public class PropertyDomainRangeAnalyser {
    private static final Logger LOGGER = LogManager.getLogger(PropertyDomainRangeAnalyser.class);
    @Nonnull
    private final OWLOntology ontology;
    private final OWLReasoner reasoner;
    private OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    private AtomicInteger counter = new AtomicInteger(0);

    public PropertyDomainRangeAnalyser(@Nonnull OWLOntology ontology) {
        this.ontology = ontology;
        reasoner = ReasonerFactory.getReasoner(ontology);
    }

    public Map<OWLAxiom, OWLAxiom> getPropertyClassAxioms() {
        Map<OWLAxiom, OWLAxiom> axiomMap = new ConcurrentHashMap<>();
        Timer timer = Timer.start("getting property domain & range class expressions");

        MultiThreadProcess.runAndWait(() ->
                ontology.getAxioms().parallelStream().forEach(axiom -> {
                    OWLAxiom a = null;
                    if (axiom instanceof OWLObjectPropertyDomainAxiom)
                        a = getClassAxiom(((OWLObjectPropertyDomainAxiom) axiom).getDomain());
                    else if (axiom instanceof OWLObjectPropertyRangeAxiom)
                        a = getClassAxiom(((OWLObjectPropertyRangeAxiom) axiom).getRange());
                    else if (axiom instanceof OWLDataPropertyDomainAxiom)
                        a = getClassAxiom(((OWLDataPropertyDomainAxiom) axiom).getDomain());

                    if (a != null) {
                        axiomMap.put(a, axiom);
                    }
                }));

        timer.stopAndPrint();
        LOGGER.info("{} Axioms added to debug properties.", axiomMap.size());
        LOGGER.debug("{}", Renderer.render(axiomMap.keySet()));

        return axiomMap;
    }

    private Set<OWLAxiom> getObjectPropertyAxioms(OWLOntology ontology, OWLObjectProperty objectProperty) {
        Set<OWLAxiom> axioms = new HashSet<>();
        MultiThreadProcess.runAndWait(() -> {
            ontology.getObjectPropertyDomainAxioms(objectProperty).parallelStream().forEach(domainAxiom -> axioms.add(getClassAxiom(domainAxiom.getDomain())));
            ontology.getObjectPropertyRangeAxioms(objectProperty).parallelStream().forEach(rangeAxiom -> axioms.add(getClassAxiom(rangeAxiom.getRange())));
        });
        return axioms;
    }

    private Set<OWLAxiom> getDataPropertyAxioms(OWLOntology ontology, OWLDataProperty dataProperty) {
        Set<OWLAxiom> axioms = new HashSet<>();
        MultiThreadProcess.runAndWait(() ->
                ontology.getDataPropertyDomainAxioms(dataProperty).parallelStream().forEach(domainAxiom -> axioms.add(getClassAxiom(domainAxiom.getDomain())))
        );
        return axioms;
    }

    private OWLAxiom getClassAxiom(OWLClassExpression classExpression) {
        if (!(classExpression instanceof OWLClass)) {
            final NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, true);
            if (subClasses.isEmpty() || subClasses.isBottomSingleton()) {
                return dataFactory.getOWLEquivalentClassesAxiom(dataFactory.getOWLClass(getRandomClassIRI()), classExpression);
            }
        }
        return null;
    }

    private IRI getRandomClassIRI() {
        return IRI.create(Configs.getInstance().IRI_PREFIX, "/DomainRangeClass" + String.valueOf(counter.incrementAndGet()));
    }
}
