package com.practice.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class FileEventHandler implements EventHandler{
    public static final Logger logger = LoggerFactory.getLogger(FileEventHandler.class.getName());

    private final KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final boolean sync;

    public FileEventHandler(KafkaProducer<String, String> kafkaProducer, String topic, boolean sync) {
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
        this.sync = sync;
    }

    @Override
    public void onMessage(MessageEvent messageEvent) throws InterruptedException, ExecutionException {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, messageEvent.key(), messageEvent.value());
        // sync
        if (this.sync) {
            final RecordMetadata recordMetadata = this.kafkaProducer.send(producerRecord).get();
            // sync code - .get() blocks the .send() method until a response is received
            logger.info("\n ##### record metadata received #### \n partition: {}\noffset: {}\ntimestamp: {}", recordMetadata.partition(), recordMetadata.offset(), recordMetadata.timestamp());
        } else {
            // async
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("\n ##### record metadata received #### \n partition: {}\noffset: {}\ntimestamp: {}",
                            metadata.partition(), metadata.offset(), metadata.timestamp());
                } else {
                    logger.error("exception error from broker - {}", exception.getMessage());
                }
            });
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String topicName = "file-topic";

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.219.212:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        boolean sync = true;

        FileEventHandler fileEventHandler = new FileEventHandler(producer, topicName, sync);
        MessageEvent messageEvent = new MessageEvent("key00001", "This is a file event message.");
        fileEventHandler.onMessage(messageEvent);
    }
}
