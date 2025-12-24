package com.example.consumers;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchExample {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(3);
        CountDownLatch mainLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);

        for (int i=0; i<3; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    System.out.println("[" + Thread.currentThread().getName() + "]" + " is sleep");
                    Thread.sleep(1000);
                    readyLatch.countDown(); // 모든 worker thread가 준비할때까지 대기
                    System.out.println("[" + Thread.currentThread().getName() + "]" + " is ready and waiting for mainLatch");
                    mainLatch.await();
                    System.out.println("[" + Thread.currentThread().getName() + "]" + " is wake up");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }
        readyLatch.await(); // main thread 대기 // 모든 daemon thread가 준비될 때까지 대기
        mainLatch.countDown(); // 이걸 호출하는 즉시 is ready and waiting for mainLatch 출력된 thread 들이 깨어나서 is wake up 출력
        doneLatch.await(); // 모든 worker thread가 종료될 때까지 대기

        System.out.println("All worker threads have finished.");
    }
}
