package ir.ac.ui.ontodebugger.evaluation;

import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import ir.ac.ui.ontodebugger.Configs;
import ir.ac.ui.ontodebugger.alignments.Alignment2OWL;
import ir.ac.ui.ontodebugger.alignments.EntityResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/26/15
 */
public class AlignmentEvaluator extends ErrorSetEvaluator {
    private static final Logger LOGGER = LogManager.getLogger(AlignmentEvaluator.class);
    private final double threshold;
    private Alignment align = null;
    private Alignment refAlignment = null;

    public AlignmentEvaluator(String args) {
        final String[] strings = args.split(Configs.ARGS_DELIMITER);
        if (strings.length < 3) {
            LOGGER.error("AlignmentEvaluator needs 3 arguments: referenceAlignmentFilePath, alignmentFilePath, threshold");
        }

        AlignmentParser parser = new AlignmentParser();
        try {
            refAlignment = parser.parse(new File(strings[0].trim()).toURI());
            align = parser.parse(new File(strings[1].trim()).toURI());
            LOGGER.info("{} -> {}", String.format("%-35s", "Before Debugging"), getBaseLineEvaluation());
        } catch (AlignmentException e) {
            LOGGER.catching(e);
        }

        this.threshold = Double.valueOf(strings[2].trim());

    }

    public Evaluation getBaseLineEvaluation() {
        return evaluate(refAlignment, align);
    }

    private Evaluation evaluate(Alignment ref, Alignment align) {
        int correct = 0;
        int error = 0;
        try {
            PRecEvaluator evaluator = new PRecEvaluator(ref, align);
            evaluator.eval(new Properties());

            correct = evaluator.getCorrect();
            error = evaluator.getFound() - correct;
        } catch (AlignmentException e) {
            LOGGER.catching(e);
        }
        return new Evaluation(ref.nbCells(), correct, error);
    }

    @Override
    public Evaluation evaluate(OWLOntology ontology, Set<OWLAxiom> errorSet) {
        Alignment improvedAlignment = (Alignment) align.clone();
        EntityResolver resolver = new EntityResolver(ontology);
        Alignment2OWL alignment2OWL = new Alignment2OWL(align, resolver, threshold);
        Map<OWLAxiom, Cell> axiomCellMap = alignment2OWL.getAxiomCellMap();

        for (OWLAxiom axiom : errorSet) {
            try {
                final Cell cell = axiomCellMap.get(axiom);
                if (cell != null) {
                    improvedAlignment.remCell(cell);
                }
            } catch (AlignmentException e) {
                LOGGER.catching(e);
            }
        }

        for (Cell cell : align) {
            if (cell.getStrength() < threshold) {
                try {
                    improvedAlignment.remCell(cell);
                } catch (AlignmentException e) {
                    LOGGER.catching(e);
                }
            }
        }

        return evaluate(refAlignment, improvedAlignment);
    }
}
