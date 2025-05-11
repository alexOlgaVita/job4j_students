package ru.students.producer;

import ru.students.producer.producer.App;

public class ProducerSrv {
    public static void main(String[] args) {
        Thread consumerThread = new Thread(App::produce);
        consumerThread.start();

    }
}