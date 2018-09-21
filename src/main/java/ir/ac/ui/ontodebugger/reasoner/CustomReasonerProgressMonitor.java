package ir.ac.ui.ontodebugger.reasoner;

import ir.ac.ui.ontodebugger.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 11/25/15
 */
public class CustomReasonerProgressMonitor implements ReasonerProgressMonitor {
    private static final Logger LOGGER = LogManager.getLogger(CustomReasonerProgressMonitor.class);

    private int lastPercentage = 0;
    private long lastTime;
    private final transient ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    private long beginTime;

    @Override
    public void reasonerTaskStarted(String taskName) {
        LOGGER.debug("  {} ...", taskName);
        lastTime = bean.getCurrentThreadCpuTime();
        beginTime = lastTime;
    }

    @Override
    public void reasonerTaskStopped() {
        LOGGER.debug("    ... finished in {}", Timer.formatDuration((bean.getCurrentThreadCpuTime() - beginTime) / 1000000L));
        lastPercentage = 0;

    }

    @Override
    public void reasonerTaskProgressChanged(int value, int max) {
        String anim = "|/-\\";
        long time = bean.getCurrentThreadCpuTime();
        if (max > 0) {
            int percent = (value * 100) / max;
            if (lastPercentage != percent) {
                String string = String.format("\r%s\t%s %d%%", anim.charAt(percent % anim.length()), getProgressBar(percent), percent);
                if (percent == 100) {
                    string += "\n";
                }
                System.out.print(string);
                lastTime = time;
                lastPercentage = percent;
            }
        }
    }

    private String getProgressBar(int percent) {

        StringBuilder bar = new StringBuilder();
        for (int x = 1; x <= 100; x++) {
            if (x <= percent)
                bar.append("=");
            else
                bar.append(" ");
        }
        return "[" + bar.toString() + "]";
    }

    @Override
    public void reasonerTaskBusy() {
        LOGGER.debug("    busy ...");
    }
}
