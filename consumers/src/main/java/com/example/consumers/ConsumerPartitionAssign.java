package com.example.consumers;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConsumerPartitionAssign {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerPartitionAssign.class);
    public static void main(String[] args) {
        String topic = "pizza-topic";

        Consumer<String, String> consumer = initKafkaConsumer();

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
        //pollAutoCommit(consumer);

        pollCommitSync(consumer);
        //pollCommitAsync(consumer);
    }

    private static Consumer<String, String> initKafkaConsumer() {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.25:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group_pizza_assign_seek");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        // 특정 파티션 할당
        TopicPartition topicPartition = new TopicPartition("pizza-topic", 0);
        consumer.assign(List.of(topicPartition));
        return consumer;
    }

    private static void pollCommitAsync(final Consumer<String, String> consumer) {
        try {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));// 1초
                logger.info("####### consumerRecords count:{}", consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key: {}, record value: {}, partition: {}, offset: {}", record.key(), record.value(), record.partition(), record.offset());
                }

                consumer.commitAsync(new OffsetCommitCallback() { // 비동기 커밋일때 오류 처리
                    @Override
                    public void onComplete(Map<TopicPartition, OffsetAndMetadata> map, Exception e) {
                        if (e != null) {
                            logger.error("offsets: {} is not completed error", map, e);
                        }
                    }
                });

            }
        } catch (WakeupException e) {
            logger.error("wakeup exception has been called");
        } finally {
            try (consumer) {
                consumer.commitSync(); // commitAsync는 실패할 수 있고 실패하면 offset이 유실될 수 있기 때문에 종료 시점에 commitSync로 한번 더 커밋
            } catch (CommitFailedException e) {
                logger.error(e.getMessage());
            } finally {
                logger.info("finally consumer is closing");
            }
        }
    }

    private static void pollCommitSync(final Consumer<String, String> consumer) {
        try {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));// 1초
                logger.info("####### consumerRecords count:{}", consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key: {}, record value: {}, partition: {}, offset: {}", record.key(), record.value(), record.partition(), record.offset());
                }
                try {
                    consumer.commitSync();
                    logger.info("####### commitSync complete #######");
                }catch (CommitFailedException e) { // 더 이상 retry 할 수 없을 때
                    logger.error(e.getMessage());
                }
            }
        } catch (WakeupException e) {
            logger.error("wakeup exception has been called");
        } finally {
            consumer.close();
            logger.info("finally consumer is closing");
        }
    }

    static void pollAutoCommit(Consumer<String, String> consumer) {
        try {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));// 1초
                logger.info("####### consumerRecords count:{}", consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key: {}, record value: {}, partition: {}, offset: {}", record.key(), record.value(), record.partition(), record.offset());
                }

                try {
                    logger.info("main thread is sleeping {} ms during while loop", 10000);
                    Thread.sleep(10000L);
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
