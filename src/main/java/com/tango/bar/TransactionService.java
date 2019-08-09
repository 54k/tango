package com.tango.bar;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TransactionService {

    private static final int SECONDS_PER_MINUTE = 60;
    private final AtomicReference[] window = new AtomicReference[SECONDS_PER_MINUTE];

    public TransactionService() {
        for (int i = 0; i < SECONDS_PER_MINUTE; i++) {
            window[i] = new AtomicReference();
        }
    }

    private static long currentMillis() {
        return TimeMachine.nowMillis();
    }

    private static int getWindowIdx(long timestamp) {
        return (int) (timestamp / 1000) % SECONDS_PER_MINUTE;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        long now = currentMillis();
        for (int i = 0; i < SECONDS_PER_MINUTE; i++) {
            long timestamp = now - i * 1000;
            int windowIdx = getWindowIdx(timestamp);
            window[windowIdx].set(new TransactionSummary(timestamp, 0, 0, 0, 0, 0));
        }
    }

    public TransactionSummary getStatistics() {
        long now = currentMillis();
        TransactionSummary total = new TransactionSummary(0, 0, 0, 0, 0, 0);

        for (AtomicReference reference : window) {
            TransactionSummary summary = (TransactionSummary) reference.get();
            if (summary != null && now - summary.getTimestamp() < SECONDS_PER_MINUTE * 1000) {
                total = addSummary(total, summary);
            }
        }

        return total;
    }

    private TransactionSummary addSummary(TransactionSummary transactionSummary1, TransactionSummary transactionSummary2) {
        double max = Math.max(transactionSummary1.getMax(), transactionSummary2.getMax());
        double min;

        if (transactionSummary1.getMin() == 0) {
            min = transactionSummary2.getMin();
        } else if (transactionSummary2.getMin() == 0) {
            min = transactionSummary1.getMin();
        } else {
            min = Math.min(transactionSummary1.getMin(), transactionSummary2.getMin());
        }

        if (min == 0) {
            min = max;
        }

        double sum = BigDecimal.valueOf(transactionSummary1.getSum())
                .add(BigDecimal.valueOf(transactionSummary2.getSum()))
                .doubleValue();
        long count = transactionSummary1.getCount() + transactionSummary2.getCount();

        double avg = sum == 0 ? 0 : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN).doubleValue();

        return new TransactionSummary(0, max, min, avg, sum, count);
    }

    @SuppressWarnings("unchecked")
    public boolean addTransaction(Transaction transaction) {
        int windowIdx = getWindowIdx(transaction.getTimestamp());
        AtomicReference reference = window[windowIdx];

        while (true) {
            TransactionSummary prevTransactionSummary = (TransactionSummary) reference.get();
            TransactionSummary newTransactionSummary = computeSummary(prevTransactionSummary, transaction);

            if (prevTransactionSummary == newTransactionSummary) {
                return false;
            }

            if (reference.compareAndSet(prevTransactionSummary, newTransactionSummary)) {
                return true;
            }
        }
    }

    private TransactionSummary computeSummary(TransactionSummary prevTransactionSummary, Transaction transaction) {
        long now = currentMillis();

        if (transaction.getAmount() == 0 ||
                transaction.getTimestamp() > now ||
                now - transaction.getTimestamp() >= SECONDS_PER_MINUTE * 1000) {
            return prevTransactionSummary;
        }

        if (prevTransactionSummary == null || now - prevTransactionSummary.getTimestamp() > SECONDS_PER_MINUTE * 1000) {
            prevTransactionSummary = new TransactionSummary(transaction.getTimestamp(), 0, 0, 0, 0, 0);
        }

        double max = Math.max(prevTransactionSummary.getMax(), transaction.getAmount());
        double min = Math.min(prevTransactionSummary.getMin(), transaction.getAmount());

        if (min == 0) {
            min = max;
        }

        double sum = BigDecimal.valueOf(prevTransactionSummary.getSum())
                .add(BigDecimal.valueOf(transaction.getAmount()))
                .doubleValue();
        long count = prevTransactionSummary.getCount() + 1;

        return new TransactionSummary(prevTransactionSummary.getTimestamp(), max, min, 0, sum, count);
    }
}