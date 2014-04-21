package ai.context.util.io;

import java.util.concurrent.ArrayBlockingQueue;

public class Channel<T> {

    private ArrayBlockingQueue<T> queue;

    public Channel(int size) {
        queue = new ArrayBlockingQueue<T>(size);
    }

    public synchronized void put(T in) {
        queue.add(in);
        if (queue.size() == 1) {
            notify();
        }
    }

    public synchronized T get() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return queue.poll();
    }
}
