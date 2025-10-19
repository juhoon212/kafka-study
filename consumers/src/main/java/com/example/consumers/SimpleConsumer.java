package com.example.consumers;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class SimpleConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleConsumer.class);
    public static void main(String[] args) {
        String topic = "simple-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.25:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "simple-group");
        //heartbeat
        props.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "5000"); // 5초마다 heartbeat 전송
        props.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "90000"); // 90초 동안 heartbeat 응답 없으면 해당 컨슈머 종료 처리
        props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "600000"); // poll() 메서드 최대 호출 간격 10분

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(topic));

        try (consumer) {
            while (true) {
                final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));// 1초
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key: {}, record value: {}, partition: {}", record.key(), record.value(), record.partition());
                }
            }
        }


    }
}
