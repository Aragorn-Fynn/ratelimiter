package ratelimiter;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * date: 2023/7/18 13:38
 * description:
 * 算法：1. 如果在当前统计时间区间内， 且请求不超过限制， 则成功， 且计数器加1
 *      2. 如果在当前统计时间区间内， 且请求超过显示，  则失败
 *      3. 如果已经过了统计时间区间， 则重置计数器和时间区间
 * 缺点：临界流量： 假如第一秒的后500ms， 第二秒的前500ms， 都产生了capacity个请求， 会导致这一秒总流量超过capacity
 */
public class CounterRateLimiter {
    private Clock clock;
    private long capacity;

    private AtomicInteger counter = new AtomicInteger(0);

    private CounterRateLimiter() {
        this.clock = Clock.createStarted();
    }

    public static CounterRateLimiter create(long capacity) {
        CounterRateLimiter counterRateLimiter = new CounterRateLimiter();
        counterRateLimiter.capacity = capacity;
        return counterRateLimiter;
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public synchronized boolean tryAcquire(int permits) {
        long elapsedSecs = clock.elapsed(TimeUnit.SECONDS);
        if (elapsedSecs >= 1) {
            counter = new AtomicInteger(0);
            clock.reset();
            clock.start();
        }

        return counter.addAndGet(permits) > capacity ? false : true;
    }

    //线程池，用于多线程模拟测试
    static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {

        CounterRateLimiter counterLimiter = CounterRateLimiter.create(5);
        // 线程数
        final int threads = 5;
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
                            System.out.println("success, time: " + LocalDateTime.now() + ", turn: " + j);
                        } else {
                            System.out.println("failed, time: " + LocalDateTime.now() + ", turn: " + j);
                            Thread.sleep(200);
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
