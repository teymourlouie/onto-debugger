package ir.ac.ui.ontodebugger.alignments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/26/15
 */
public class EntityResolver {
    private static final Logger LOGGER = LogManager.getLogger(EntityResolver.class);
    private final List<OWLOntology> ontologies = new ArrayList<>();

    public EntityResolver(OWLOntology... ontologies) {
        for (OWLOntology ontology : ontologies) {
            this.ontologies.add(ontology);
        }
    }

    /**
     * Search for entity with the given iri within all ontologies
     *
     * @param iri
     * @return first entity with the iri or null if no entity found in none of ontologies
     */
    public OWLEntity getEntity(IRI iri) {
        for (OWLOntology ontology : ontologies) {
            Set<OWLEntity> entities = ontology.getEntitiesInSignature(iri, Imports.INCLUDED);
            if (!entities.isEmpty()) {
                return entities.iterator().next();
            }
        }
        return null;
    }

    public OWLEntity getEntity(URI uri) {
        return getEntity(IRI.create(uri));
    }
}
