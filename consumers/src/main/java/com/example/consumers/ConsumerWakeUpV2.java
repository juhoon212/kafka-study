package com.example.consumers;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerWakeUpV2 {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerWakeUpV2.class);
    public static void main(String[] args) {
        String topic = "pizza-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.25:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group_03");
        // 60초 이후 poll() 메서드가 호출되지 않으면 해당 컨슈머 종료 처리
        // consumer는 rebalance 과정에서 제외되고, 남은 컨슈머들이 파티션을 재할당 받음
        props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "60000");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(topic));

        // main thread
        Thread mainThread = Thread.currentThread();
        // main thread 종료 시 별도의 thread로 kafkaConsumer wakeup() 메소드를 호출하게 함
        // graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
            consumer.wakeup(); // poll() 메서드를 즉시 종료시키는 역할

            try {
                mainThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        int loopCnt = 0;
        // 여기가 병목되면 컨슈머 그룹에서 계속 rebalance 발생
        // 성능 저하 발생
        // 여기 있는 로직을 줄일 수 없다면 partition 수를 늘려서 병목 현상을 완화해야 함
        // 재실행되는 이유는 kafkaConsumer가 poll()을 다시 호출하기 때문 - 다시 heartbeat 전송 시작
        // 만약 데이터 양이 produce 되는 양이 많다면 partition 수를 늘려서 병목 현상을 완화해야 함
        // while loop 안에서 실행되는 코드의 양이 많거나 오래걸리면 MAX_POLL_INTERVAL_MS_CONFIG 시간을 늘려야 함
        try {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));// 1초
                logger.info("############ loopCnt: {}, consumerRecords count:{}", loopCnt++, consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key: {}, record value: {}, partition: {}, offset: {}", record.key(), record.value(), record.partition(), record.offset());
                }

                try {
                    logger.info("main thread is sleeping {} ms during while loop", loopCnt*10000);
                    Thread.sleep(loopCnt * 10000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (WakeupException e) {
            logger.error("wakeup exception has been called");
        } finally {
            consumer.close();
            logger.info("finally consumer is closing");
        }
    }
}
