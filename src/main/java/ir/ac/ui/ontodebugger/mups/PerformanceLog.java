package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.util.Timer;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Mehdi Teymourlouie (mehdi.teymourlouie@gmail.com)
 * on 2/24/18.
 */
@Data
public class PerformanceLog {
    AtomicLong findRandomMUPSTime = new AtomicLong(0);
    AtomicLong satisfiableChecksTime = new AtomicLong(0);

    AtomicLong expandTime = new AtomicLong(0);
    AtomicLong shrinkTime = new AtomicLong(0);


    @Override
    public String toString() {
        return "PerformanceLog{" +
                "findRandomMUPSTime=" + Timer.getFormattedTime(findRandomMUPSTime.longValue()) +
                ", satisfiableChecksTime=" + Timer.getFormattedTime(satisfiableChecksTime.longValue()) +
                ", expandTime=" + Timer.getFormattedTime(expandTime.longValue()) +
                ", shrinkTime=" + Timer.getFormattedTime(shrinkTime.longValue()) +
                '}';
    }
}
