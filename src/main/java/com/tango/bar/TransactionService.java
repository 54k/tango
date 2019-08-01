package com.tango.bar;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TransactionService {

    private static final int SECONDS_PER_MINUTE = 60;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final TransactionSummary[] window = new TransactionSummary[SECONDS_PER_MINUTE];

    private static long currentSecondMillis() {
        return withZeroNanos(System.currentTimeMillis());
    }

    private static long withZeroNanos(long millis) {
        return millisToSeconds(millis) * 1000;
    }

    private static long millisToSeconds(long millis) {
        return millis / 1000;
    }

    @PostConstruct
    public void init() {
        Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {
            for (int i = 0; i < window.length; i++) {
                long timestamp = currentSecondMillis() - i * 1000;
                int windowIdx = (int) (millisToSeconds(timestamp) % window.length);
                window[windowIdx] = new TransactionSummary(timestamp, 0, 0, 0, 0, 0);
            }
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedRate = 1000L)
    public void advanceWindow() {
        doAddTransaction(new Transaction(0, System.currentTimeMillis()));
    }

    public TransactionSummary getStatistics() {
        Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            TransactionSummary result = new TransactionSummary(0, 0, 0, 0, 0, 0);
            for (TransactionSummary transactionSummary : window) {
                result = addSummary(result, transactionSummary);
            }
            return result;
        } finally {
            lock.unlock();
        }
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

        if (min == 0.0) {
            min = max;
        }

        double volume = transactionSummary1.getVolume() + transactionSummary2.getVolume();
        long count = transactionSummary1.getCount() + transactionSummary2.getCount();
        double avg = volume == 0.0 ? 0.0 : volume / count;

        return new TransactionSummary(0, max, min, avg, volume, count);
    }

    public void addTransaction(Transaction transaction) {
        if (withZeroNanos(transaction.getTimestamp()) > currentSecondMillis()) {
            return;
        }

        doAddTransaction(transaction);
    }

    private void doAddTransaction(Transaction transaction) {
        Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {
            long timestamp = withZeroNanos(transaction.getTimestamp());
            int windowIdx = (int) (millisToSeconds(timestamp) % window.length);
            TransactionSummary prevTransactionSummary = window[windowIdx];
            window[windowIdx] = computeSummary(prevTransactionSummary, transaction);
        } finally {
            lock.unlock();
        }
    }

    private TransactionSummary computeSummary(TransactionSummary prevTransactionSummary, Transaction transaction) {
        if (prevTransactionSummary.getTimestamp() > transaction.getTimestamp()) {
            return prevTransactionSummary;
        }

        if (prevTransactionSummary.getTimestamp() < withZeroNanos(transaction.getTimestamp())) {
            prevTransactionSummary = new TransactionSummary(withZeroNanos(transaction.getTimestamp()), 0, 0, 0, 0, 0);
        }

        double max = Math.max(prevTransactionSummary.getMax(), transaction.getAmount());
        double min = Math.min(prevTransactionSummary.getMin(), transaction.getAmount());
        if (min == 0) {
            min = max;
        }

        double volume = prevTransactionSummary.getVolume() + transaction.getAmount();
        long count = prevTransactionSummary.getCount() + (transaction.getAmount() == 0.0 ? 0 : 1);

        double avg = volume == 0.0 ? 0.0 : volume / count;

        return new TransactionSummary(prevTransactionSummary.getTimestamp(), max, min, avg, volume, count);
    }
}
