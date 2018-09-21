package ir.ac.ui.ontodebugger.alignments;

import aml.AML;
import aml.match.Alignment;
import aml.ontology.Ontology2Match;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 12/8/15
 */
public class AlignmentGenerator {
    private static final Logger LOGGER = LogManager.getLogger(AlignmentGenerator.class);
    private final OWLOntology source;
    private final OWLOntology target;
    @Getter
    private Alignment alignment = null;

    public AlignmentGenerator(@Nonnull OWLOntology source, @Nonnull OWLOntology target) {
        this.source = source;
        this.target = target;
    }

    public OWLOntology generate(double threshold) {
        LOGGER.info("Generating alignment between {} and {}", source.getOntologyID(), target.getOntologyID());

        AML aml = AML.getInstance();
        aml.setOntologies(new Ontology2Match(source), new Ontology2Match(target));
        aml.matchAuto();
        alignment = aml.getAlignment();

        if (alignment == null) {
            return null;
        }

        final String file = Long.toHexString(Double.doubleToLongBits(Math.random())) + ".rdf";
        saveAs(file);

        final OWLOntology ontology = loadAlignment(file, threshold);

        File temp = new File(file);
        if (temp.exists()) {
            temp.delete();
        }
        return ontology;
    }

    public OWLOntology loadAlignment(String file, double threshold) {
        final EntityResolver resolver = new EntityResolver(source, target);
        AlignmentLoader loader = new AlignmentLoader(file);
        return loader.getOWLOntology(resolver, threshold);
    }


    public void saveAs(String file) {
        if (alignment == null) {
            return;
        }

        try {
            alignment.saveRDF(file);
        } catch (FileNotFoundException e) {
            LOGGER.catching(e);
        }
    }
}
