package com.tango.bar;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<Void> transactions(@RequestBody Transaction transaction) {
        boolean isAdded = transactionService.addTransaction(transaction);
        HttpStatus status = isAdded ? HttpStatus.CREATED : HttpStatus.NO_CONTENT;
        return ResponseEntity.status(status).build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<TransactionSummary> statistics() {
        return ResponseEntity.ok(transactionService.getStatistics());
    }
}
