package course.concurrency.exams;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MyBlockingQueueTest {

    private final MyBlockingQueue<Object> queue = new MyBlockingQueue<>();
    private ExecutorService executor1 = Executors.newFixedThreadPool(5);
    private ExecutorService executor2 = Executors.newFixedThreadPool(5);

    @Test
    void emptyQueueReadWriteMultithreadingTest() {
        readWriteQueue();

        assertEquals(0, queue.getSize());
    }

    @Test
    void fullQueueReadWriteMultithreadingTest() {
        fullQueue();
        readWriteQueue();

        assertEquals(10, queue.getSize());
    }

    @Test
    void shouldBlockOnRead() {
        Future<?> future = executor1.submit(() -> {
            queue.dequeue();
        });

        assertThrows(TimeoutException.class, () ->  future.get(10, TimeUnit.SECONDS));
        assertEquals(0, queue.getSize());
    }

    @Test
    void shouldBlockOnWrite() {
        fullQueue();

        Future<?> future = executor1.submit(() -> {
            queue.enqueue(new Object());
        });

        assertThrows(TimeoutException.class, () ->  future.get(10, TimeUnit.SECONDS));
        assertEquals(10, queue.getSize());
    }

    @Test
    void shouldBeFifoQueue() {
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();

        queue.enqueue(o1);
        queue.enqueue(o2);
        queue.enqueue(o3);

        assertEquals(o1, queue.dequeue());
        assertEquals(o2, queue.dequeue());
        assertEquals(o3, queue.dequeue());
    }

    @Test
    void testGetSize() {
        queue.enqueue(new Object());
        queue.enqueue(new Object());
        queue.enqueue(new Object());
        queue.enqueue(new Object());

        assertEquals(4, queue.getSize());

        queue.dequeue();
        queue.dequeue();

        assertEquals(2, queue.getSize());
    }

    private void fullQueue() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(new Object());
        }
    }

    private void readWriteQueue() {
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < 5; i++) {
            executor1.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < 10; it++) {
                    queue.enqueue(new Object());
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            executor2.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < 10; it++) {
                    queue.dequeue();
                }
            });
        }

        latch.countDown();

        executor1.shutdown();
        executor2.shutdown();
        try {
            executor1.awaitTermination(10, TimeUnit.SECONDS);
            executor2.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
