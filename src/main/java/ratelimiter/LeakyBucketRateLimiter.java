package ratelimiter;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * @author: chengfei.feng
 * date: 2023/7/19 9:38
 * description: 漏桶算法
 */
public class LeakyBucketRateLimiter {
    private volatile long nextTime;

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
        this.nextTime = System.nanoTime();
    }

    public static LeakyBucketRateLimiter create(long rate, int capacity) {
        LeakyBucketRateLimiter rateLimiter = new LeakyBucketRateLimiter();
        rateLimiter.rate = rate;
        rateLimiter.capacity = capacity;
        rateLimiter.lock = new Semaphore(capacity);
        rateLimiter.gap = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS) / rate;
        return rateLimiter;
    }

    public boolean tryAcquire() {
        if (lock.tryAcquire()) {
            boolean res = tryAcquire(1);
            lock.release();
            return res;
        } else {
            return false;
        }
    }

    private boolean tryAcquire(int permits) {
        try {
            synchronized (this) {
                nextTime += gap;
            }
            Thread.sleep(TimeUnit.MILLISECONDS.convert(nextTime - System.nanoTime(), TimeUnit.NANOSECONDS));
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    //线程池，用于多线程模拟测试
    static final int threads = 5;
    static ExecutorService pool = Executors.newFixedThreadPool(threads);

    public static void main(String[] args) {


        LeakyBucketRateLimiter counterLimiter = LeakyBucketRateLimiter.create(1L, 2);
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

                        boolean result = counterLimiter.tryAcquire();
                        if (result) {
                            System.out.println( "thread: ["+Thread.currentThread().getId() + "] success, time: [" + LocalDateTime.now() + " ], " +
                                    "turn: " + j);
                        } else {
                            System.out.println("thread: ["+Thread.currentThread().getId() + "] failed, time: [" + LocalDateTime.now() + "], turn: " + j);
                            Thread.sleep(1000);
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
