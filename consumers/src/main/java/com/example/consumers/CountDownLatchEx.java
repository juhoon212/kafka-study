package com.example.consumers;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchEx {

    public static void main(String[] args) throws InterruptedException {
                int consumerCount = 3;
                String topic = "pizza-topic";
                CountDownLatch readyLatch = new CountDownLatch(consumerCount); // consumer 준비 완료 대기
                CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호
                CountDownLatch doneLatch = new CountDownLatch(consumerCount); // 모든 consumer 종료 대기

                for (int i = 0; i < consumerCount; i++) {
                    final int id = i;
                    new Thread(() -> {
                        Properties props = new Properties();
                        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.25:9092");
                        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group_latch_test");
                        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                            consumer.subscribe(Collections.singleton(topic));
                            System.out.println("[Consumer-" + id + "] subscribed to topic. Ready.");

                            readyLatch.countDown(); // 구독 준비 완료
                            startLatch.await(); // main thread의 start 신호 대기

                            System.out.println("[Consumer-" + id + "] started polling...");
                            while (true) {
                                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                                for (ConsumerRecord<String, String> record : records) {
                                    System.out.printf("[Consumer-%d] key=%s, value=%s, partition=%d, offset=%d%n",
                                            id, record.key(), record.value(), record.partition(), record.offset());
                                }
                            }
                        } catch (InterruptedException e) {
                            System.out.println("[Consumer-" + id + "] interrupted, shutting down...");
                        } finally {
                            doneLatch.countDown();
                        }
                    }).start();
                }

                // 모든 consumer가 준비될 때까지 대기
                readyLatch.await();
                System.out.println("✅ All consumers ready! Sending start signal...");
                startLatch.countDown(); // 모든 consumer에 시작 신호 발사

                // main thread는 이후 종료까지 기다릴 수도 있음
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("🧹 Shutdown detected, waiting for consumers to close...");
                    try {
                        doneLatch.await();
                    } catch (InterruptedException ignored) {}
                    System.out.println("✅ All consumers terminated. Exiting.");
                }));
            }
}
