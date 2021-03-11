package service.cloud.connections;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PingResult {
    public Instant startTime;
    public Instant finishTime;

    /**
     * A ping result with a run time of one day, to flag errors.
     */
    public static final PingResult ERROR_PING_RESULT;

    static {
        ERROR_PING_RESULT = new PingResult();
        ERROR_PING_RESULT.finishTime = Instant.now();
        ERROR_PING_RESULT.startTime = ERROR_PING_RESULT.finishTime.minus(1, ChronoUnit.DAYS);
    }

    public long getRunTimeInMillis() {
        return finishTime.toEpochMilli() - startTime.toEpochMilli();
    }
}
