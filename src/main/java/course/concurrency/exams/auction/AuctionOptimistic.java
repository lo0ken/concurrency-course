package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    public boolean propose(Bid bid) {
        Bid oldBid = latestBid.get();

        Bid current;
        do {
            current = latestBid.get();
        } while ((current == null || bid.getPrice() > current.getPrice()) && latestBid.compareAndSet(current, bid));

        if (oldBid != latestBid.get()) {
            notifier.sendOutdatedMessage(oldBid);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
