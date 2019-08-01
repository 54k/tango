package com.tango.bar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceTest {

    @Autowired TransactionService service;

    @Before
    public void setUp() {
        service.init();
    }

    @Test
    public void testService() {
        // given: 'transactions with timestamp within last 15 seconds'
        List<Transaction> transactions = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            long timestamp = getTimestamp(-random.nextInt(15));
            Transaction transaction = new Transaction(1.0 + i, timestamp);
            transactions.add(transaction);
        }

        // when: 'add transactions'
        transactions.forEach(service::addTransaction);
        // add transaction with timestamp in the past
        service.addTransaction(new Transaction(100, getTimestamp(-61)));
        // add transaction with timestamp in the future
        service.addTransaction(new Transaction(100, getTimestamp(2)));

        // then: 'get expected summary'
        TransactionSummary summary = service.getStatistics();
        assertThat(summary.getCount(), equalTo(5L));
        assertThat(summary.getMax(), equalTo(5.0));
        assertThat(summary.getMin(), equalTo(1.0));
        assertThat(summary.getVolume(), equalTo(15.0));
        assertThat(summary.getAvg(), equalTo(3.0));
    }

    private static long getTimestamp(long secondsAdjust) {
        return System.currentTimeMillis() + secondsAdjust * 1000;
    }
}
