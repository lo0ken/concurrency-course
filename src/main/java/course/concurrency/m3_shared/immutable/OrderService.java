package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private AtomicLong nextId = new AtomicLong(1L);

    private long nextId() {
        return nextId.getAndIncrement();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = new Order(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        var order = currentOrders.computeIfPresent(orderId, (k, v) -> v.withPaymentInfo(paymentInfo));
        if (order.checkStatus()) {
            deliver(order);
        }
    }

    public void setPacked(long orderId) {
        var order = currentOrders.computeIfPresent(orderId, (k, v) -> v.withPacked(true));

        if (order.checkStatus()) {
            deliver(order);
        }
    }

    private void deliver(Order order) {
        /* ... */
        if (order.getStatus() != Order.Status.DELIVERED) {
            currentOrders.put(order.getId(), order.withStatus(Order.Status.DELIVERED));
        }
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
