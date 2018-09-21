/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ir.ac.ui.ontodebugger.util;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 */
public class Timer {
    private static final Logger LOGGER = LogManager.getLogger(Timer.class);

    @Getter
    private String name = "";
    // Private Members
    @Getter
    private boolean started = false;
    @Getter
    private long startTime = 0;
    private long elapsedTime = 0;

    public Timer(String name) {
        this.name = name;
    }

    public static Timer start(String name) {
        final Timer timer = new Timer(name);
        timer.start();
        return timer;
    }


    public void start() {
        if (!started) {
            startTime = getNow();
            started = true;
        }
    }

    public void stop() {
        if (started) {
            elapsedTime = getNow() - startTime;
            started = false;
        }
    }

    public void stopAndPrint() {
        stop();
        printTime();
    }

    public void reset() {
        elapsedTime = 0;
        if (started) {
            startTime = getNow();
        }
    }

    private long getNow() {
        return System.nanoTime();
    }

    public long getElapsedTimeNanos() {
        if (started) {
            return getNow() - startTime;
        }
        return elapsedTime;
    }

    public long getElapsedTimeMillis() {
        return getElapsedTimeNanos() / 1000000L;
    }

    @Override
    public String toString() {
        return name + " took: " + getTimeElapsed();
    }

    private String getTimeElapsed() {
        long millis = getElapsedTimeMillis();
        return getFormattedTime(millis);
    }

    public static String getFormattedTime(long millis) {
        return formatDuration(millis) + " (" + millis + " ms)";
    }

    /**
     * format duration in milliseconds into a human readable format
     *
     * @param millis
     * @return
     */
    public static String formatDuration(long millis) {
        String s = String.format("%02d:%02d:%02d,%03d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                millis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
        final String emptyPrefix = "00:";
        while (s.startsWith(emptyPrefix)) {
            s = s.substring(emptyPrefix.length());
        }
        return s;
    }

    public void printTime() {
        LOGGER.info(this.toString());
    }


    public long getTotal() {
        return getElapsedTimeMillis();
    }
}
