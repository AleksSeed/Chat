package ru.geekbrain.ChatServer.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 1. Создать три потока, каждый из которых выводит определенную букву (A, B и C) 5 раз (порядок – ABСABСABС).
 *      Используйте wait/notify/notifyAll.*/

// - 2. На серверной стороне сетевого чата реализовать управление потоками через ExecutorService.

public class HW4 {
    static volatile char cur = 'A';
    static Object monitor = new Object();


    static class WaitNotifyClass implements Runnable {
        private char currentLetter;
        private char nextLetter;

        public WaitNotifyClass(char currentLetter, char nextLetter) {
            this.currentLetter = currentLetter;
            this.nextLetter = nextLetter;
        }

        @Override
        public void run() {
            ExecutorService executorService = Executors.newFixedThreadPool(3);

          //  executorService.execute(() -> {
                for (int i = 0; i < 5; i++) {
                    synchronized (monitor) {
                        try {
                            while (cur != currentLetter)
                                monitor.wait();
                            System.out.print(currentLetter);
                            cur = nextLetter;
                            monitor.notifyAll();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
              //  executorService.shutdown();
            }
        }

        public static void main(String[] args) {
            System.out.println("Задание 1:");
            new Thread(new WaitNotifyClass('A', 'B')).start();
            new Thread(new WaitNotifyClass('B', 'C')).start();
            new Thread(new WaitNotifyClass('C', 'A')).start();

        }
    }




