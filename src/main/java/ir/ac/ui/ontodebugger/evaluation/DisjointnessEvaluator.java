package ir.ac.ui.ontodebugger.evaluation;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/22/15
 */
public class DisjointnessEvaluator extends ErrorSetEvaluator {
    private static final Logger LOGGER = LogManager.getLogger(DisjointnessEvaluator.class);
    public static final String ONTOLOGY_IRI = "http://dbpedia.org/ontology/";
    /**
     * Incorrect Axioms in Reference ontology
     */
    private Set<OWLAxiom> incorrectAxioms;

    /**
     * Incorrect Axioms in ontology based on Reference ontology
     */


    public DisjointnessEvaluator(String minusFilename) {
        try {
            incorrectAxioms = parseDisjointGoldStandardFile(minusFilename);
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }

    public static Set<OWLAxiom> parseDisjointGoldStandardFile(String file) throws IOException {
        Set<OWLAxiom> axioms = new HashSet<>();
        final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

        final List<String> list = FileUtils.readLines(new File(file), Charset.defaultCharset());
        list.stream().forEach(s -> {
            final String[] names = s.split("__");
            if (names.length != 2) {
                LOGGER.error("Unexpected line: {}", s);
            }
            IRI c1 = IRI.create(ONTOLOGY_IRI, names[0]);
            IRI c2 = IRI.create(ONTOLOGY_IRI, names[1]);
            final OWLDisjointClassesAxiom axiom = dataFactory.getOWLDisjointClassesAxiom(dataFactory.getOWLClass(c1), dataFactory.getOWLClass(c2));
            axioms.add(axiom);
        });
        return axioms;
    }

    @Override
    public Evaluation evaluate(OWLOntology ontology, Set<OWLAxiom> errorSet) {
        errorAxioms = getDisjointAxioms(ontology).stream().filter(axiom -> incorrectAxioms.contains(axiom.getAxiomWithoutAnnotations())).collect(Collectors.toSet());


        trueAxioms = errorSet.stream().filter(errorAxioms::contains).collect(Collectors.toSet());
        falseAxioms = errorSet.stream().filter(axiom -> !trueAxioms.contains(axiom)).collect(Collectors.toSet());

        Evaluation evaluation = new Evaluation();
        evaluation.setTotalAxiomCount(ontology.getAxiomCount());
        evaluation.setTotalErrorCount(errorAxioms.size());
        evaluation.setTrueErrorCount(trueAxioms.size());
        evaluation.setFalseErrorCount(falseAxioms.size());

        return evaluation;
    }

    private static Set<OWLAxiom> getDisjointAxioms(OWLOntology ontology) {
        return ontology.getAxioms().stream().filter(axiom -> axiom.isOfType(AxiomType.DISJOINT_CLASSES)).collect(Collectors.toSet());
    }


}
