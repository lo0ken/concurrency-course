package course.concurrency.exams;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<T>{

    private static final int MAX_SIZE = 10;
    private final LinkedList<T> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private volatile int size = 0;

    public void enqueue(T value) {
        lock.lock();
        try {
            while (queue.size() == MAX_SIZE) {
                try {
                    notFull.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            queue.add(value);
            size++;
        } finally {
            lock.unlock();
        }

    }
    public T dequeue() {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                try {
                    notEmpty.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            var el = queue.removeFirst();
            notFull.signal();
            size--;
            return el;
        } finally {
            lock.unlock();
        }
    }

    public int getSize() {
        return size;
    }
}
