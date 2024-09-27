package course.concurrency.m3_shared.immutable;

import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private final Long id;
    private final List<Item> items;
    private PaymentInfo paymentInfo;
    private boolean isPacked;
    private Status status;

    public Order(Long id, List<Item> items) {
        this.id = id;
        this.items = items;
        this.status = NEW;
    }

    private Order (Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = items;
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public boolean checkStatus() {
        if (!items.isEmpty() && paymentInfo != null && isPacked) {
            return true;
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }

    public Order withStatus(Order.Status status) {
        return new Order(id, items, paymentInfo, isPacked, status);
    }

    public Order withPacked(boolean isPacked) {
        return new Order(id, items, paymentInfo, isPacked, status);
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(id, items, paymentInfo, isPacked, status);
    }
}
