package ru.students.consumer.consumer;

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

@Slf4j
public class App {

    public static void consume() {
        var config = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            config.load(input);
        } catch (IOException e) {
            log.error("Error reading file: ", e);
        }
        /* настройки для консьюмера */
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getProperty("bootstrap.servers"));
        /* можно формировать уникальный group.id: props.setProperty("group.id", UUID.randomUUID().toString()); */
        props.setProperty("group.id", config.getProperty("group.id"));
        props.setProperty("key.deserializer", config.getProperty("key.deserializer"));
        props.setProperty("value.deserializer", config.getProperty("value.deserializer"));
        props.setProperty("enable.auto.commit", config.getProperty("enable.auto.commit"));
        props.setProperty("auto.commit.interval.ms", config.getProperty("auto.commit.interval.ms"));
        /* настройки для продьюсера - записи в ответный топик ответа об обработке полученного сообщения */
        Properties producerProps = new Properties();
        producerProps.setProperty("bootstrap.servers", config.getProperty("bootstrap.servers"));
        producerProps.setProperty("key.serializer", config.getProperty("key.serializer"));
        producerProps.setProperty("value.serializer", config.getProperty("value.serializer"));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Arrays.asList(config.getProperty("topic")));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Received message: " + record.value());
                    System.out.printf("received message: %s\n", record.value());
                    String response = "Получено и обработано сообщение: " + record.value();
                    ProducerRecord<String, String> responseRecord = new ProducerRecord<>(config.getProperty("responseTopic"), record.key(), response);
                    /* этот же correlationId записываем в сообщение ответного топика, чтобы по нему идентифицировать сообщение, обработку которого ожидаем
                    (в отправляющем сервисе)*/
                    responseRecord.headers().add("correlationId", record.headers().lastHeader("correlationId").value());
                    /* вывод хедеров - для отладки */
                    for (Header header : responseRecord.headers()) {
                        System.out.println("header key " + header.key() + "header value " + new String(header.value()));
                    }
                    try (Producer<String, String> producer = new KafkaProducer<>(producerProps)) {
                        producer.send(responseRecord);
                    } catch (Exception e) {
                        log.error("Could not start responce producer: ", e);
                    }
                }
            }
        }
    }
}
