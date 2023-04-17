package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid = new Bid(111L, 111L, Long.MIN_VALUE);

    private volatile boolean stopped = false;

    public boolean propose(Bid bid) {
        if (!stopped && bid.getPrice() > latestBid.getPrice()) {
            synchronized (this) {
                if (!stopped && bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        stopped = true;
        return latestBid;
    }
}