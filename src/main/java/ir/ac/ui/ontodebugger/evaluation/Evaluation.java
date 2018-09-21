package ir.ac.ui.ontodebugger.evaluation;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/22/15
 */
public class Evaluation {
    private static final Logger LOGGER = LogManager.getLogger(Evaluation.class);

    @Getter
    @Setter
    private int trueErrorCount = 0;
    @Getter
    @Setter
    private int falseErrorCount = 0;
    @Getter
    @Setter
    private int totalErrorCount = 0;
    @Getter
    @Setter
    private int totalAxiomCount = 0;

    public Evaluation(int totalErrorCount, int trueErrorCount, int falseErrorCount) {
        this.totalErrorCount = totalErrorCount;
        this.trueErrorCount = trueErrorCount;
        this.falseErrorCount = falseErrorCount;
    }

    public Evaluation() {

    }


    /**
     * determines what is the recall of incorrect axioms
     *
     * @return number of true incorrect axiom count divided by total incorrect axiom count
     */
    public double getRecall() {
        final int totalCount = getTotalErrorCount();
        if (totalCount == 0) {
            return 1;
        } else {
            return getTrueErrorCount() / (double) totalCount;
        }
    }

    /**
     * determines what is the precision of the detected axioms
     *
     * @return number of true incorrect axiom count divided by total detected axioms
     */
    public double getPrecision() {
        final int totalCount = getTrueErrorCount() + getFalseErrorCount();
        if (totalCount == 0) {
            return 1;
        } else {
            return getTrueErrorCount() / (double) totalCount;
        }
    }

    public double getAccuracy() {
        return 1 - getErrorRate();
    }

    public double getErrorRate() {
        final int totalCount = totalAxiomCount - getTotalErrorCount();
        if (totalCount == 0) {
            if (getFalseErrorCount() == 0) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return getFalseErrorCount() / (double) totalCount;
        }
    }

    public double getFMeasure() {
        final double precision = getPrecision();
        final double recall = getRecall();
        double v = precision + recall;
        if (Math.abs(v) < 0.0001) {
            return 0;
        } else {
            return 2 * (precision * recall) / v;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("#Total Errors: " + getTotalErrorCount());
        sb.append("\t(Precision, Recall, F-measure, #Detected, #False Axioms)");
        sb.append("\t" + String.format("%.3f", getPrecision()));
        sb.append("\t" + String.format("%.3f", getRecall()));
        sb.append("\t" + String.format("%.3f", getFMeasure()));
        sb.append("\t" + getTrueErrorCount());
        sb.append("\t" + getFalseErrorCount());

        return sb.toString();
    }
}
