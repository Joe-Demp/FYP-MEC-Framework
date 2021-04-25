package ie.ucd.mecframework.metrics.latency;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PingResult {
    /**
     * A ping result with a run time of one day, to flag errors.
     */
    public static final PingResult ERROR_PING_RESULT;

    static {
        ERROR_PING_RESULT = new PingResult();
        ERROR_PING_RESULT.finishTime = Instant.now();
        ERROR_PING_RESULT.startTime = ERROR_PING_RESULT.finishTime.minus(1, ChronoUnit.DAYS);
    }

    public Instant startTime;
    public Instant finishTime;

    public long getRunTimeInMillis() {
        return finishTime.toEpochMilli() - startTime.toEpochMilli();
    }
}
