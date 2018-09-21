package ir.ac.ui.ontodebugger.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 8/4/15.
 */
public class ConfigLogger {

    public static final String DEFAULT_LOG_FILE = "log.txt";
    public static final String DEFAULT_RESULT_FILE = "result.txt";
    public static final String RESULTS_PROPERTY_NAME = "debuggerResultsLogFile";
    public static final String LOG_PROPERTY_NAME = "debuggerLogFile";
    public static final String NAME_SEPARATOR = "-";

    private ConfigLogger() {

    }

    /**
     * Set the result filename, it does not affect logger until {@link #reconfigure()} is called.
     *
     * @param filename
     */
    public static void setResultFile(String filename) {
        System.setProperty(RESULTS_PROPERTY_NAME, filename);
    }

    /**
     * set the result basename and reconfigure the logger
     *
     * @param basename
     */
    public static void reconfigureResultFile(String basename) {
        setResultFile(basename + NAME_SEPARATOR + DEFAULT_RESULT_FILE);

        reconfigure();
    }

    /**
     * Set log filename, it does not affect logger until {@link #reconfigure()} is called.
     *
     * @param filename
     */
    public static void setLogFile(String filename) {
        System.setProperty(LOG_PROPERTY_NAME, filename);
    }

    /**
     * set log filename and reconfigure the logger
     *
     * @param basename
     */
    public static void reconfigureLogFile(String basename) {
        setLogFile(basename + NAME_SEPARATOR + DEFAULT_LOG_FILE);

        reconfigure();
    }

    public static void reconfigureFilenames(String basename) {
        setResultFile(basename + NAME_SEPARATOR + DEFAULT_RESULT_FILE);
        setLogFile(basename + NAME_SEPARATOR + DEFAULT_LOG_FILE);
        reconfigure();
    }

    /**
     * Reconfigure the logger;
     * if the log file names are changed via {@link #setResultFile(String)} or {@link #setLogFile(String)}, calling this
     * method affect the logger
     */
    public static void reconfigure() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }
}
