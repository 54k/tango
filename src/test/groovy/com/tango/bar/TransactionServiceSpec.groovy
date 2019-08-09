package com.tango.bar

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransactionServiceSpec extends Specification {

    static final ObjectMapper MAPPER = new ObjectMapper()

    @Autowired TransactionService service
    @Autowired MockMvc mvc

    void setup() {
        service.clear()
        TimeMachine.reset()
    }

    void testRestController() {
        given: 'transactions with timestamp within last 15 seconds'
            TimeMachine.setDate(LocalDateTime.now().withNano(0))

            def random = new Random()
            List<Transaction> transactions = (1..5).collect {
                def timestamp = getTimestamp(-random.nextInt(60))
                new Transaction(timestamp, it)
            }

        when:
            def createdRequests = transactions.collect {
                mvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(it))
                ).andExpect(status().isCreated())
            }

        then:
            createdRequests.each {
                it.andExpect(status().isCreated())
            }

        when:
            def noContentRequests = [1, -61].collect {
                def body = asJsonString(new Transaction(getTimestamp(it), 100))
                mvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
            }

        then:
            noContentRequests.each {
                it.andExpect(status().isNoContent())
            }

        when:
            def statisticsRequest = mvc.perform(get("/statistics"))

        then:
            statisticsRequest
                    .andExpect(status().isOk())
                    .andExpect(content().json(asJsonString(new TransactionSummary(0, 5, 1, 3, 15, 5))))

    }

    void testService() {
        given:
            TimeMachine.setDate(LocalDateTime.now().withNano(0))

            def random = new Random()
            List<Transaction> transactions = (1..5).collect {
                def timestamp = getTimestamp(-random.nextInt(15))
                new Transaction(timestamp, it)
            }

        when:
            def added = transactions.collect(service.&addTransaction)
        then:
            added.every()

        when:
            def notAdded = [1, -61].collect {
                service.addTransaction(new Transaction(getTimestamp(it), 100))
            }
        then:
            notAdded.every { !it }

        when:
            def summary = service.getStatistics()
        then:
            summary.count == 5
            summary.max == 5
            summary.min == 1
            summary.sum == 15
            summary.avg == 3
    }

    void testWindowAdvance() {
        def now = LocalDateTime.now()
        TimeMachine.setDate(now.withNano(0))
        given:
            service.addTransaction(new Transaction(getTimestamp(-60), 100))
        when:
            TimeMachine.setDate(now.plusSeconds(1))
        then:
            service.getStatistics().count == 0
    }

    private static long getTimestamp(long secondsAdjust) {
        return TimeMachine.nowMillis() + secondsAdjust * 1000
    }

    static String asJsonString(Object obj) {
        return MAPPER.writeValueAsString(obj)
    }
}
