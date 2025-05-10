package com.bank.loanapi.controller;

import com.bank.loanapi.model.Loan;
import com.bank.loanapi.model.LoanInstallment;
import com.bank.loanapi.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/create")
    public ResponseEntity<Loan> createLoan(@RequestBody Map<String, String> request) {
        Long customerId = Long.valueOf(request.get("customerId"));
        BigDecimal amount = new BigDecimal(request.get("amount"));
        BigDecimal interestRate = new BigDecimal(request.get("interestRate"));
        int installments = Integer.parseInt(request.get("installments"));

        Loan loan = loanService.createLoan(customerId, amount, interestRate, installments);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Loan>> getLoansByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(loanService.getLoansByCustomerId(customerId));
    }

    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<LoanInstallment>> getInstallments(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getInstallmentsByLoanId(loanId));
    }

    @PostMapping("/{loanId}/pay")
    public ResponseEntity<Map<String, Object>> payLoan(@PathVariable Long loanId, @RequestBody Map<String, String> request) {
        BigDecimal amount = new BigDecimal(request.get("amount"));
        Map<String, Object> result = loanService.payLoan(loanId, amount);
        return ResponseEntity.ok(result);
    }
}
