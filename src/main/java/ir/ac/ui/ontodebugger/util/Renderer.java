package ir.ac.ui.ontodebugger.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.SimpleRenderer;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/15/15
 */
public class Renderer {
    private static final Logger LOGGER = LogManager.getLogger(Renderer.class);

    private Renderer() {
    }

    public static SimpleRenderer getSimpleRenderer() {
        SimpleRenderer r = new SimpleRenderer();
        r.setShortFormProvider(new SimpleShortFormProvider());
        return r;
    }

    public static String render(@Nonnull OWLAxiom axiom) {
        if (axiom == null)
            return "null";
        else
            return getSimpleRenderer().render(axiom.getAxiomWithoutAnnotations());
    }

    public static String render(OWLObject clazz) {
        return getSimpleRenderer().render(clazz);
    }

    public static String render(Collection<OWLAxiom> owlAxioms) {
        return render(owlAxioms, "\n");
    }

    public static String render(Collection<OWLAxiom> owlAxioms, String separator) {
        if (owlAxioms == null)
            return "";

        StringBuilder builder = new StringBuilder();
        owlAxioms.stream().sorted().forEachOrdered(owlAxiom -> builder.append(separator + Renderer.render(owlAxiom)));

        if (builder.length() > separator.length())
            return builder.toString().substring(separator.length());
        else
            return "\n";
    }
}
