package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(111L, 111L, Long.MIN_VALUE));

    public boolean propose(Bid bid) {
        Bid oldBid = latestBid.get();

        Bid current;

        do {
            current = latestBid.get();
            if (bid.getPrice() < current.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(current, bid));

        notifier.sendOutdatedMessage(oldBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
