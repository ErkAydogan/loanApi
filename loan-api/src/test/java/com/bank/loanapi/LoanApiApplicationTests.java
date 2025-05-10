package com.bank.loanapi;

import com.bank.loanapi.model.Customer;
import com.bank.loanapi.model.Loan;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.LoanInstallmentRepository;
import com.bank.loanapi.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanInstallmentRepository installmentRepository;

    private Customer testCustomer;

    @BeforeEach
    void setup() {
        customerRepository.deleteAll();
        loanRepository.deleteAll();
        installmentRepository.deleteAll();

        testCustomer = Customer.builder()
                .name("Test")
                .surname("User")
                .creditLimit(new BigDecimal("50000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .build();

        testCustomer = customerRepository.save(testCustomer);
    }

    @Test
    void testCreateLoan_Success() {
        BigDecimal amount = new BigDecimal("12000");
        BigDecimal interest = new BigDecimal("0.2");
        int installments = 12;

        Loan loan = loanService.createLoan(testCustomer.getId(), amount, interest, installments);

        assertNotNull(loan.getId());
        assertEquals(installments, loan.getNumberOfInstallment());
        assertEquals(loan.getCustomer().getId(), testCustomer.getId());
        assertEquals(new BigDecimal("14400.00"), loan.getLoanAmount());
    }
}
