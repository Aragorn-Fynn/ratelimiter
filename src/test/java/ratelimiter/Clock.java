package ratelimiter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * date: 2023/7/18 13:40
 * description:
 */
public class Clock {
    private long startTime;
    private long elapsedNanos;
    private boolean isRunning;

    private Clock() {
        this.elapsedNanos = 0;
        this.isRunning = false;
    }

    public static Clock createStarted() {
        Clock clock = new Clock();
        clock.start();
        return clock;
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        startTime = System.nanoTime();
    }

    public void stop() {
        isRunning = false;
        elapsedNanos = System.nanoTime() - startTime + elapsedNanos;
    }

    public void reset() {
        elapsedNanos = 0;
        isRunning = false;
    }

    public long elapsedNanos() {
        return isRunning ? System.nanoTime() - startTime + elapsedNanos : elapsedNanos;
    }


    public Duration elapsed() {
        return Duration.ofNanos(elapsedNanos());
    }

    /**
     * 返回计时开始后过去的时间。
     */
    public long elapsed(TimeUnit disiredUnit) {
        return disiredUnit.convert(elapsedNanos(), TimeUnit.NANOSECONDS);
    }

    public boolean isRunning() {
        return isRunning;
    }

}
