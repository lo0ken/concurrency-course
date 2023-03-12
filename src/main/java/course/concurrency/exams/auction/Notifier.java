package course.concurrency.exams.auction;

import java.util.concurrent.*;

public class Notifier {

    private final ExecutorService executor = Executors.newFixedThreadPool(200);

    public void sendOutdatedMessage(Bid bid) {
        CompletableFuture
                .runAsync(this::imitateSending, executor);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}