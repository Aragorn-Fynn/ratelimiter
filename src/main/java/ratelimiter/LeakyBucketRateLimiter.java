package ratelimiter;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: chengfei.feng
 * date: 2023/7/19 9:38
 * description: 漏桶算法
 */
public class LeakyBucketRateLimiter {
    /**
     * next time of running out of the bucket;
     */
    private volatile AtomicLong nextTime;

    /**
     * 流出速率
     */
    private long rate;

    /**
     * 流出间隔
     */
    private long gap;

    /**
     * 漏桶容量
     */
    private long capacity;

    private Semaphore lock;

    private LeakyBucketRateLimiter() {
        this.nextTime = new AtomicLong(System.nanoTime());
    }

    public static LeakyBucketRateLimiter create(long rate, int capacity) {
        LeakyBucketRateLimiter rateLimiter = new LeakyBucketRateLimiter();
        rateLimiter.rate = rate;
        rateLimiter.capacity = capacity;
        rateLimiter.lock = new Semaphore(capacity, true);
        rateLimiter.gap = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS) / rate;
        return rateLimiter;
    }

    public boolean tryAcquire() {
        if (lock.tryAcquire()) {
            boolean res = doTryAcquire();
            lock.release();
            return res;
        } else {
            return false;
        }
    }

    private boolean doTryAcquire() {
        try {
            long waitTime;
            synchronized (this) {
                // reset next time
                if (System.nanoTime() > nextTime.get() + gap * capacity) {
                    nextTime.set(System.nanoTime());
                }
                // the time of wait.
                waitTime = nextTime.addAndGet(gap) - System.nanoTime();
            }

            TimeUnit.NANOSECONDS.sleep(waitTime);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    //线程池，用于多线程模拟测试
    static final int threads = 5;
    static ExecutorService pool = Executors.newFixedThreadPool(threads);

    public static void main(String[] args) {


        LeakyBucketRateLimiter counterLimiter = LeakyBucketRateLimiter.create(2L, 2);
        // 线程数
        // 每条线程的执行轮数
        final int turns = 20;
        // 同步器
        CountDownLatch countDownLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {

            pool.submit(() -> {
                try {

                    for (int j = 0; j < turns; j++)
                    {
                        LocalDateTime startTime = LocalDateTime.now();
                        boolean result = counterLimiter.tryAcquire();
                        if (result) {
                            System.out.println( "thread: ["+Thread.currentThread().getId() + "] success, end time: [" + LocalDateTime.now() + " ], startTime: " + startTime);
                            Thread.sleep(3000);
                        } else {
                            System.out.println("thread: ["+Thread.currentThread().getId() + "] failed, end time: [" + LocalDateTime.now() + "], startTime: " + startTime);
                            Thread.sleep(3000);
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
                //等待所有线程结束
                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();
            pool.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
