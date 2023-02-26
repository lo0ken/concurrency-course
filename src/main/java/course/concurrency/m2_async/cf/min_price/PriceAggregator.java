package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PriceAggregator {

    private static long RETRIEVE_PRICE_TIMEOUT_MS = 2900;

    private PriceRetriever priceRetriever = new PriceRetriever();

    private ExecutorService executor = new ThreadPoolExecutor(0, 3000,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        var futures = shopIds.stream()
                .map(shopId -> retrievePrice(itemId, shopId))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(price -> !Double.isNaN(price))
                .min(Double::compareTo)
                .orElse(Double.NaN);
    }

    private CompletableFuture<Double> retrievePrice(long itemId, long shopId) {
        return CompletableFuture
                .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                .completeOnTimeout(Double.NaN, RETRIEVE_PRICE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(e -> Double.NaN);
    }
}
