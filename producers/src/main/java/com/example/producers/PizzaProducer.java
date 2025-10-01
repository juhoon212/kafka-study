package com.example.producers;

import net.datafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PizzaProducer {
    public static final Logger logger = LoggerFactory.getLogger(PizzaProducer.class.getName());

    public static void sendPizzaMessage(
            final KafkaProducer<String, String> kafkaProducer,
            final String topic,
            final int iterCount,
            final int interIntervalMs,
            final int intervalMs,
            final int intervalCount,
            boolean sync
    ) {
        PizzaMessage pizzaMessage = new PizzaMessage();
        int iterSeq = 0;
        long seed = 2022;
        Random random = new Random(seed);
        Faker faker = new Faker(random);

        while (iterCount != iterSeq++) {
            final HashMap<String, String> pMessage = pizzaMessage.produceMsg(faker, random, iterSeq);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, pMessage.get("key"), pMessage.get("message"));
            sendMessage(kafkaProducer, record, pMessage, sync);

            if ((intervalCount > 0) && (iterSeq % intervalCount == 0)) {
                try {
                    logger.info("###### intervalCount: {} intervalMillis: {}", intervalCount, intervalMs);
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }

            if (interIntervalMs > 0) {
                try {
                    logger.info("###### interIntervalMs: {}", interIntervalMs);
                    Thread.sleep(interIntervalMs);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    public static void sendMessage(
            final KafkaProducer<String, String> producer,
            final ProducerRecord<String, String> record,
            final HashMap<String, String> pMessage,
            boolean sync
    ) {
        // KafkaProducer send data to Kafka topic
        if (!sync) {
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("async message ; {}, partition: {}, offset: {} ",
                            pMessage.get("key"), metadata.partition(), metadata.offset());
                } else {
                    logger.error("exception error from broker - {}", exception.getMessage());
                }
            });
        } else {
            try {
                RecordMetadata recordMetadata = producer.send(record).get();
                logger.info("sync message ; {}, partition: {}, offset: {} ",
                        pMessage.get("key"), recordMetadata.partition(), recordMetadata.offset());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        String topic = "pizza-topic";
        // KafkaProducer configuration setting
        Properties props = new Properties();
        // boostrap.servers, key.serializer.class, value.serializer.class
        //props.setProperty("bootstrap.servers", "192.168.64.22:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.219.212:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //props.setProperty(ProducerConfig.ACKS_CONFIG, "0"); // 0으로 해놓으면 브로커로 부터 acks 를 기다리지 않기 때문에 offset 정보를 받을 수 없음
        //batch settings
        //props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "32000");
        //props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");

        // KafkaProducer object creation
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        sendPizzaMessage(producer, topic, -1, 10, 100, 100, true);
        producer.close();
    }
}
