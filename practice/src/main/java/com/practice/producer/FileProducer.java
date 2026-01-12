package com.practice.producer;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class FileProducer {
    public static final Logger logger = LoggerFactory.getLogger(FileProducer.class.getName());
    public static void main(String[] args) {
        String topic = "file-topic";
        // KafkaProducer configuration setting
        Properties props = new Properties();
        // boostrap.servers, key.serializer.class, value.serializer.class
        //props.setProperty("bootstrap.servers", "192.168.64.22:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.219.212:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer object creation
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        String filePath = "/Users/yunjoohoon/Desktop/study/boot-study-own/kafka-proj-01/practice/src/main/resources/pizza_sample.txt";

        // ProducerRecord 객체 생성
        sendFileMessages(producer, topic, filePath);

        // send() 비동기 방식 전송

        producer.close();
    }

    private static void sendFileMessages(final KafkaProducer<String, String> producer, final String topic, final String filePath) {
        String line;
        final String delimiter = ",";
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(delimiter);
                String key = tokens[0];
                StringBuilder value = new StringBuilder();

                for (int i=1; i<tokens.length; ++i) {
                    if (i == tokens.length - 1) value.append(tokens[i]);
                    else value.append(tokens[i]).append(",");
                }
                sendMessage(producer, topic, key, String.valueOf(value));
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void sendMessage(
            final KafkaProducer<String, String> producer,
            final String topic,
            final String key,
            final String value
    ) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, String.valueOf(key), value);
        logger.info("key: {}, value: {}", key, value);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                logger.info("####### record metadata received #### \n partition: {}\noffset: {}\ntimestamp: {}",
                        metadata.partition(), metadata.offset(), metadata.timestamp());
            } else logger.error("exception error from broker - {}", exception.getMessage());
        });
    }
}
