package ru.students.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.students.consumer.consumer.App;

public class ConsumerSrv {
    public static void main(String[] args) {

        Thread consumerThread = new Thread(App::consume);
        consumerThread.start();
    }
}