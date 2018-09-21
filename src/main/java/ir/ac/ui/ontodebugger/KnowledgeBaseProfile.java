package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.reasoner.ReasonerFactory;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/11/15
 */
public class KnowledgeBaseProfile {
    private static final Logger LOGGER = LogManager.getLogger(KnowledgeBaseProfile.class);
    private final OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();

    @Getter
    private final Collection<OWLOntology> ontologyList = new HashSet<>();
    @Getter
    private final OWLOntology profileOntology;
    private final Map<OWLOntology, String> ontologyPathes = new ConcurrentHashMap<>();

    public KnowledgeBaseProfile(@Nonnull String knowledgeBasePath) throws OWLOntologyCreationException {
        OntologyHelper.getListOfOntologyFiles(knowledgeBasePath).parallelStream().forEach(file -> {

            final OWLOntology ontology = OntologyHelper.loadOntology(file, manager);
            if (ontology != null) {
                try {
                    manager.removeAxioms(ontology, ontology.getABoxAxioms(Imports.INCLUDED));
                    checkOntologyCoherency(ontology);
                } catch (Exception ex) {
                    LOGGER.error("Cannot load ontology: {} \n {}", file, ex.getMessage());
                    manager.removeOntology(ontology);
                }
                ontologyList.add(ontology);
                ontologyPathes.put(ontology, file.getName());
            }

        });
        LOGGER.info("{} ontologies loaded as profile", ontologyList.size());

        // build merged profile ontology
        profileOntology = manager.createOntology(IRI.create(Configs.IRI_PREFIX, "/profile/"));
        ontologyList.stream().forEach(ontology -> manager.addAxioms(profileOntology, ontology.getAxioms()));
        LOGGER.info("Profile Ontology:{}", OntologyHelper.getOntologyInfo(profileOntology));
        checkOntologyCoherency(profileOntology);
    }

    private void checkOntologyCoherency(OWLOntology ontology) {
        if (Configs.getInstance().isCHECK_PROFILE_ONTOLOGY_SATISFIABILITY()) {
            OWLReasoner reasoner = ReasonerFactory.getReasoner(ontology);
            LOGGER.info("Ontology {} # Incoherent Classes: {}", ontology.getOntologyID(), reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size());
        }
    }

    public boolean contains(OWLAxiom axiom) {
        return ontologyList.stream().anyMatch(ontology -> ontology.containsAxiom(axiom));
    }

    public String getOntologyFileName(OWLOntology ontology) {
        return ontologyPathes.get(ontology);
    }
}
