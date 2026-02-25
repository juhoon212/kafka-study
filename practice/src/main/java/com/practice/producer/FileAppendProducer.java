package com.practice.producer;

import com.practice.event.EventHandler;
import com.practice.event.FileEventHandler;
import com.practice.event.FileEventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class FileAppendProducer {
    private static final Logger logger = LoggerFactory.getLogger(FileAppendProducer.class.getName());
    public static void main(String[] args) {
        String topic = "file-topic";
        // KafkaProducer configuration setting
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.25:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer object creation
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        boolean sync = false;

        File file = new File("/Users/yunjoohoon/Desktop/study/boot-study-own/kafka-proj-01/practice/src/main/resources/pizza_append.txt");
        EventHandler eventHandler = new FileEventHandler(producer, topic, sync);
        FileEventSource fileEventSource = new FileEventSource(true, 1000, file, eventHandler);

        Thread fileEventSourceThread = new Thread(fileEventSource);
        fileEventSourceThread.start();

        try {
            fileEventSourceThread.join();
        } catch (InterruptedException e) {
            logger.error("file event source thread has been interrupted");
        } finally {
            producer.close();
        }
    }
}
