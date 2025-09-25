package com.example.producers;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerASyncCustomCB {
    public static final Logger logger = LoggerFactory.getLogger(ProducerASyncCustomCB.class.getName());
    public static void main(String[] args) {
        String topic = "multipart-topic";
        // KafkaProducer configuration setting
        Properties props = new Properties();
        // boostrap.servers, key.serializer.class, value.serializer.class
        //props.setProperty("bootstrap.servers", "192.168.64.22:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.219.212:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer object creation
        KafkaProducer<Integer, String> producer = new KafkaProducer<>(props);
        // ProducerRecord object creation
        for (int i=0; i<20; ++i) {
            Callback callback = new CustomCallback(i);
            ProducerRecord<Integer, String> record = new ProducerRecord<>(topic, i, "hello-world" + i);
            // KafkaProducer send data to Kafka topic
            producer.send(record, callback);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        producer.close();
    }
}
