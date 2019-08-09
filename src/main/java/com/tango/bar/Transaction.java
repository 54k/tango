package com.tango.bar;

public final class Transaction {

    private final long timestamp;
    private final double amount;

    public Transaction(long timestamp, double amount) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }
}
