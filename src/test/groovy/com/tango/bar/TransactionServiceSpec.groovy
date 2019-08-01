package com.tango.bar

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class TransactionServiceSpec extends Specification {

    @Autowired TransactionService service

    void setup() {
        service.init()
    }

    void testService() {
        given: 'transactions with timestamp within last 15 seconds'
            def random = new Random()
            List<Transaction> transactions = (1..5).collect {
                def timestamp = getTimestamp(-random.nextInt(15))
                new Transaction(it, timestamp)
            }

        when: 'add transactions'
            transactions.each(service.&addTransaction)
            // add transaction with timestamp in the past
            service.addTransaction(new Transaction(100, getTimestamp(-61)))
            // add transaction with timestamp in the future
            service.addTransaction(new Transaction(100, getTimestamp(2)))

        then: 'get expected summary'
            def summary = service.getStatistics()

            summary.count == 5
            summary.max == 5
            summary.min == 1
            summary.volume == 15
            summary.avg == 3
    }

    private static long getTimestamp(long secondsAdjust) {
        return System.currentTimeMillis() + secondsAdjust * 1000
    }
}
