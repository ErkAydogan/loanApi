package com.bank.loanapi.service;

import com.bank.loanapi.model.*;
import com.bank.loanapi.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final List<Integer> ALLOWED_INSTALLMENTS = List.of(6, 9, 12, 24);
    private static final BigDecimal MIN_INTEREST = new BigDecimal("0.1");
    private static final BigDecimal MAX_INTEREST = new BigDecimal("0.5");
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;

    @Transactional
    public Loan createLoan(Long customerId, BigDecimal amount, BigDecimal interestRate, int numberOfInstallments) {
        if (!ALLOWED_INSTALLMENTS.contains(numberOfInstallments)) {
            throw new IllegalArgumentException("Installment count must be 6, 9, 12, or 24");
        }

        if (interestRate.compareTo(MIN_INTEREST) < 0 || interestRate.compareTo(MAX_INTEREST) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0.1 and 0.5");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BigDecimal totalAmount = amount.multiply(BigDecimal.ONE.add(interestRate));
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        if (customer.getCreditLimit().subtract(customer.getUsedCreditLimit()).compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Not enough credit limit");
        }

        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalAmount));
        customerRepository.save(customer);

        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(totalAmount)
                .numberOfInstallment(numberOfInstallments)
                .createDate(LocalDate.now())
                .isPaid(false)
                .build();

        loan = loanRepository.save(loan);

        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2, RoundingMode.HALF_UP);
        List<LoanInstallment> installments = new ArrayList<>();

        LocalDate dueDate = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth());

        for (int i = 0; i < numberOfInstallments; i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .dueDate(dueDate.plusMonths(i))
                    .isPaid(false)
                    .build();
            installments.add(installment);
        }

        installmentRepository.saveAll(installments);
        return loan;
    }

    public List<Loan> getLoansByCustomerId(Long customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    public List<LoanInstallment> getInstallmentsByLoanId(Long loanId) {
        return installmentRepository.findByLoanIdOrderByDueDateAsc(loanId);
    }

    @Transactional
    public Map<String, Object> payLoan(Long loanId, BigDecimal paymentAmount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        List<LoanInstallment> installments = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loanId);

        LocalDate today = LocalDate.now();
        int paidCount = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (LoanInstallment installment : installments) {
            if (installment.isPaid()) continue;

            if (installment.getDueDate().isAfter(today.plusMonths(3))) continue;

            BigDecimal finalAmount = installment.getAmount();

            if (today.isBefore(installment.getDueDate())) {
                long earlyDays = installment.getDueDate().toEpochDay() - today.toEpochDay();
                BigDecimal discount = installment.getAmount()
                        .multiply(BigDecimal.valueOf(0.001))
                        .multiply(BigDecimal.valueOf(earlyDays));
                finalAmount = finalAmount.subtract(discount);
            } else if (today.isAfter(installment.getDueDate())) {
                long lateDays = today.toEpochDay() - installment.getDueDate().toEpochDay();
                BigDecimal penalty = installment.getAmount()
                        .multiply(BigDecimal.valueOf(0.001))
                        .multiply(BigDecimal.valueOf(lateDays));
                finalAmount = finalAmount.add(penalty);
            }

            finalAmount = finalAmount.setScale(2, RoundingMode.HALF_UP);

            if (paymentAmount.compareTo(finalAmount) >= 0) {
                installment.setPaid(true);
                installment.setPaymentDate(today);
                installment.setPaidAmount(finalAmount);
                installmentRepository.save(installment);

                paymentAmount = paymentAmount.subtract(finalAmount);
                totalSpent = totalSpent.add(finalAmount);
                paidCount++;
            } else {
                break;
            }
        }

        boolean allPaid = installmentRepository
                .findByLoanIdOrderByDueDateAsc(loanId)
                .stream()
                .allMatch(LoanInstallment::isPaid);

        if (allPaid) {
            loan.setIsPaid(true);
            loanRepository.save(loan);
        }

        Customer customer = loan.getCustomer();
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(totalSpent));
        customerRepository.save(customer);

        return Map.of(
                "installmentsPaid", paidCount,
                "totalSpent", totalSpent,
                "loanFullyPaid", allPaid
        );
    }

}
