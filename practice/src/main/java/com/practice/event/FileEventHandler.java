package com.practice.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
