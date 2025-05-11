package ru.students.producer.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class App {

    /**
     * Для релизации синхронной обработки (+ без использования спринга) реализуем концепцию:
     * - используя хедер ("correlationId") при записи в топик запроса и в топик ответа;
     * - во избежание бесконечного цикла выставляем ограничение в виде таймаутеа.
     */
    public static void produce() {
        var config = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            config.load(input);
        } catch (IOException e) {
            log.error("Error reading file:", e);
        }
        /* настройки для продьюсера */
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getProperty("bootstrap.servers"));
        props.setProperty("key.serializer", config.getProperty("key.serializer"));
        props.setProperty("value.serializer", config.getProperty("value.serializer"));
        /* настройки для консьмера - получения сообщения-подтверждения об обработке отправленного сообщения */
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", config.getProperty("bootstrap.servers"));
        consumerProps.setProperty("group.id", config.getProperty("group.id"));
        consumerProps.setProperty("key.deserializer", config.getProperty("key.deserializer"));
        consumerProps.setProperty("value.deserializer", config.getProperty("value.deserializer"));
        consumerProps.setProperty("enable.auto.commit", config.getProperty("enable.auto.commit"));
        consumerProps.setProperty("auto.commit.interval.ms", config.getProperty("auto.commit.interval.ms"));
        /* настройки для вычитки сообщений*/
        int maxTime = Integer.parseInt(config.getProperty("replyMaxTime"));
        int poolDuration = Integer.parseInt(config.getProperty("poolDuration"));
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 1000; i++) {
                String key = Integer.toString(i);
                String message = "This is message " + Integer.toString(i);
                ProducerRecord<String, String> record = new ProducerRecord<>(config.getProperty("topic"), key, message);
                var correlationId = UUID.randomUUID().toString();
                /* record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8)); */
                record.headers().add("correlationId", correlationId.getBytes());
                producer.send(record);
                log.info("Sent msg with key: " + key + ", value: " + message + ", correlationId: " + correlationId);
                var isFound = false;
                var wastedTime = 0;
                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
                    consumer.subscribe(Arrays.asList(config.getProperty("responseTopic")));
                    while (!isFound && wastedTime <= maxTime) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(poolDuration));
                        for (ConsumerRecord<String, String> consRecord : records) {
                            wastedTime += poolDuration;
                            Header header = record.headers().lastHeader("correlationId");
                            var isEq = header != null ? new String(header.value()).equals(correlationId) : false;
                            if (isEq) {
                                log.info("Response received about message processing: " + consRecord.value());
                                isFound = true;
                                break;
                            }
                        }
                    }
                }
                if (!isFound) {
                    log.error("A processing message with correlationId = " + correlationId + " not found");
                }
            }
        } catch (Exception e) {
            log.error("Could not start producer: ", e);
        }
    }
}
