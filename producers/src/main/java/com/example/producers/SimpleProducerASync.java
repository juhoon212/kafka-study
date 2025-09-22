package com.example.producers;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SimpleProducerASync {
    public static final Logger logger = LoggerFactory.getLogger(SimpleProducerASync.class.getName());
    public static void main(String[] args) {
        String topic = "simple-topic";
        // KafkaProducer configuration setting
        Properties props = new Properties();
        // boostrap.servers, key.serializer.class, value.serializer.class
        //props.setProperty("bootstrap.servers", "192.168.64.22:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.219.212:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer object creation
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        // ProducerRecord object creation
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, "id-001", "hello-world");

        // KafkaProducer send data to Kafka topic
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                logger.info("\n ##### record metadata received #### \n partition: {}\noffset: {}\ntimestamp: {}",
                        metadata.partition(), metadata.offset(), metadata.timestamp());
            } else {
                logger.error("exception error from broker - {}", exception.getMessage());
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        producer.close();
    }
}
