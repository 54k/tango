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
        init();
    }

    private static long currentSecondMillis() {
        return withZeroNanos(TimeMachine.nowMillis());
    }

    private static long withZeroNanos(long millis) {
        return millisToSeconds(millis) * 1000;
    }

    private static long millisToSeconds(long millis) {
        return millis / 1000;
    }

    @SuppressWarnings("unchecked")
    private void init() {
        long now = currentSecondMillis();
        for (int i = 0; i < SECONDS_PER_MINUTE; i++) {
            long timestamp = now - i * 1000;
            int windowIdx = (int) (millisToSeconds(timestamp) % SECONDS_PER_MINUTE);
            window[windowIdx].set(new TransactionSummary(timestamp, 0, 0, 0, 0, 0));
        }
    }

    public void clear() {
        init();
    }

    private void advanceWindow() {
        long now = currentSecondMillis();
        for (int i = 0; i < SECONDS_PER_MINUTE; i++) {
            long timestamp = now - i * 1000;
            doAddTransaction(new Transaction(timestamp, 0.0));
        }
    }

    public TransactionSummary getStatistics() {
        advanceWindow();
        TransactionSummary summary = new TransactionSummary(0, 0, 0, 0, 0, 0);
        for (AtomicReference reference : window) {
            summary = addSummary(summary, (TransactionSummary) reference.get());
        }

        return summary;
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

        double sum = transactionSummary1.getSum() + transactionSummary2.getSum();
        long count = transactionSummary1.getCount() + transactionSummary2.getCount();

        double avg = sum == 0 ? 0 : new BigDecimal(sum).divide(new BigDecimal(count), RoundingMode.FLOOR).doubleValue();

        return new TransactionSummary(0, max, min, avg, sum, count);
    }

    public boolean addTransaction(Transaction transaction) {
        if (withZeroNanos(transaction.getTimestamp()) > currentSecondMillis()) {
            return false;
        }

        return doAddTransaction(transaction);
    }

    @SuppressWarnings("unchecked")
    private boolean doAddTransaction(Transaction transaction) {
        long timestamp = withZeroNanos(transaction.getTimestamp());
        int windowIdx = (int) (millisToSeconds(timestamp) % SECONDS_PER_MINUTE);
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
        if (prevTransactionSummary.getTimestamp() > transaction.getTimestamp()) {
            return prevTransactionSummary;
        }

        if (prevTransactionSummary.getTimestamp() < withZeroNanos(transaction.getTimestamp())) {
            prevTransactionSummary = new TransactionSummary(withZeroNanos(transaction.getTimestamp()), 0, 0, 0, 0, 0);
        }

        if (transaction.getAmount() == 0) {
            return prevTransactionSummary;
        }

        double max = Math.max(prevTransactionSummary.getMax(), transaction.getAmount());
        double min = Math.min(prevTransactionSummary.getMin(), transaction.getAmount());

        if (min == 0) {
            min = max;
        }

        double sum = prevTransactionSummary.getSum() + transaction.getAmount();
        long count = prevTransactionSummary.getCount() + 1;

        return new TransactionSummary(prevTransactionSummary.getTimestamp(), max, min, 0, sum, count);
    }
}