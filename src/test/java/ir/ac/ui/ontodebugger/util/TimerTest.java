package ir.ac.ui.ontodebugger.util;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 3/5/16.
 */
public class TimerTest {

    @Test
    public void testStart() throws Exception {
        final String name = "timer for test";
        Timer timer = new Timer(name);
        timer.start();
        testStartedTimerObject(name, timer);
    }

    @Test
    public void testStaticStart() throws Exception {
        final String name = "test timer";
        final Timer timer = Timer.start(name);
        testStartedTimerObject(name, timer);
    }

    private void testStartedTimerObject(String name, Timer timer) {
        assertNotNull(timer, "Timer object is null");
        assertTrue(timer.isStarted(), "Timer is not started");
        assertEquals(timer.getName(), name, "Timer name is not stored well");

        timer.stop();
        assertFalse(timer.isStarted(), "timer is not stopped after calling stop method");
        assertNotEquals(timer.getElapsedTimeMillis(), 0, "timer duration is Zero!!!");
    }

    @Test
    public void testStop() throws Exception {

    }

    @Test
    public void testFormatDuration() throws Exception {
        long duration = 100;
        assertEquals(Timer.formatDuration(duration), "00,100",
                "Error in formatting duration below a second");

        duration = 26 * 1000 + 250;
        assertEquals(Timer.formatDuration(duration), "26,250",
                "Error in formatting duration below a minute");

        duration = (3 * 60 + 26) * 1000 + 250;
        assertEquals(Timer.formatDuration(duration), "03:26,250",
                "Error in formatting duration below an hour");

        duration = ((6 * 60 + 3) * 60 + 26) * 1000 + 250;
        assertEquals(Timer.formatDuration(duration), "06:03:26,250",
                "Error in formatting full duration");
    }
}
