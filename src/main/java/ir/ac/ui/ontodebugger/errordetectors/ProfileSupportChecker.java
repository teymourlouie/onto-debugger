package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 11/5/15
 */
public class ProfileSupportChecker {
    private static final Logger LOGGER = LogManager.getLogger(ProfileSupportChecker.class);

    private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private List<OWLOntology> ontologyList = new ArrayList<>();

    public ProfileSupportChecker() {
        // constructor
    }

    public void init(KnowledgeBaseProfile profile, OWLOntology alignment) {
        // in cases that fini is not called before it ensures release of previously used resources
        fini();

        // add ontologies in profile to the list
        addOntology(profile.getProfileOntology(), alignment);
        profile.getOntologyList().forEach(ontology -> addOntology(ontology, alignment));
    }

    private void addOntology(OWLOntology ontology, OWLOntology alignment) {
        try {
            final OWLOntology ont = manager.createOntology(ontology.getAxioms());
            if (alignment != null) {
                // alignments make connections between axioms to be evaluated and the profile ontologies
                manager.addAxioms(ont, alignment.getAxioms());
            }
            this.ontologyList.add(ont);
        } catch (OWLOntologyCreationException e) {
            LOGGER.catching(e);
        }
    }

    public void fini() {
        ontologyList.stream().forEach(ontology -> {
            ReasonerFactory.removeReasoner(ontology);
            manager.removeOntology(ontology);
        });
        ontologyList.clear();
    }

    public List<SupportStatus> getAxiomSupportStatus(OWLAxiom axiom) {
        return ontologyList.stream().map(ontology -> getAxiomSupportStatus(ontology, axiom)).collect(Collectors.toList());
    }

    private SupportStatus getAxiomSupportStatus(OWLOntology ontology, OWLAxiom axiom) {
        OWLReasoner reasoner = ReasonerFactory.getReasoner(ontology, true);
        if (reasoner.isEntailed(axiom)) {
            return SupportStatus.AXIOM_ENTAILED;
        } else {
            int initUnSatClasses = reasoner.getUnsatisfiableClasses().getSize();
            manager.addAxiom(ontology, axiom);
            reasoner.flush();
            boolean negationEntailed = !reasoner.isConsistent() || reasoner.getUnsatisfiableClasses().getSize() != initUnSatClasses;
            manager.removeAxiom(ontology, axiom);
            if (negationEntailed) {
                return SupportStatus.NEGATION_ENTAILED;
            } else {
                return SupportStatus.UNKNOWN;
            }
        }
    }
}
