package ir.ac.ui.ontodebugger.util;

import ir.ac.ui.ontodebugger.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 11/24/15
 */
public class MultiThreadProcess {
    private static final Logger LOGGER = LogManager.getLogger(MultiThreadProcess.class);
    public static final ForkJoinPool forkJoinPool;

    static {
        if (Configs.getInstance().getNUMBER_OF_THREADS() == 0) {
            forkJoinPool = ForkJoinPool.commonPool();
        } else {
            forkJoinPool = new ForkJoinPool(Configs.getInstance().getNUMBER_OF_THREADS());
        }
    }

    private MultiThreadProcess() {
    }

    public static void runAndWait(Runnable task) {
        try {
            forkJoinPool.submit(task).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.catching(e);
        }

    }

    public static void runAndWaitWithTimeout(Runnable task, long timeout, TimeUnit timeUnit) {
        try {
            forkJoinPool.submit(task).get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.catching(e);
        }

    }

    public static Object runAndWaitWithTimeout(Callable<? extends Object> task, long timeout, TimeUnit timeUnit) {
        try {
            return forkJoinPool.submit(task).get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.catching(e);
        }

        return null;
    }


}
