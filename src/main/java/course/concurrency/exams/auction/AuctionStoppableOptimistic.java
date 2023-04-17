package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(new Bid(111L, 111L, Long.MIN_VALUE), false);

    private volatile boolean stopped = false;

    public boolean propose(Bid bid) {
        Bid oldBid = latestBid.getReference();

        Bid current;

        do {
            current = latestBid.getReference();
            if (latestBid.isMarked() || bid.getPrice() < current.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(current, bid, false, stopped));

        notifier.sendOutdatedMessage(oldBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        stopped = true;
        Bid current;

        do {
            current = latestBid.getReference();
        } while (!latestBid.attemptMark(current, true));

        return current;
    }
}
