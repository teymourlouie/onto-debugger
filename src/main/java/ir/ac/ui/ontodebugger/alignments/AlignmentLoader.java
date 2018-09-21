package ir.ac.ui.ontodebugger.alignments;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/12/15
 */
public class AlignmentLoader {
    private static final Logger LOGGER = LogManager.getLogger(AlignmentLoader.class);

    @Getter
    private Alignment alignment = null;

    public AlignmentLoader(String filename) {
        this(new File(filename));
    }

    public AlignmentLoader(@Nonnull File file) {
        if (file.exists() && file.isFile()) {
            try {
                AlignmentParser parser = new AlignmentParser();
                alignment = parser.parse(file.toURI());
            } catch (AlignmentException e) {
                LOGGER.catching(e);
                alignment = null;
            }
        } else {
            alignment = null;
        }
    }

    public OWLOntology getOWLOntology(EntityResolver resolver, double threshold) {
        if (alignment == null) {
            return null;
        }
        final Alignment2OWL converter = new Alignment2OWL(alignment, resolver, threshold);
        return converter.getOWLOntology();
    }
}
