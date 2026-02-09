package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.Transaction;
import org.example.app.domain.repository.TransactionRepository;

@Component
@Transactional(readOnly = true)
public class TransactionFacade {

    private final TransactionRepository transactionRepository;

    public TransactionFacade(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<Transaction> findPage(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public Transaction save(Transaction entity) {
        return transactionRepository.save(entity);
    }

    public List<Transaction> findByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTenantIdAndDateBetween(tenantId, startDate, endDate);
    }

    public List<Transaction> findSharedByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByTenantIdAndIsSharedTrueAndDateBetween(tenantId, startDate, endDate);
    }

    public List<Transaction> findWithInstallmentsByTenant(Long tenantId) {
        return transactionRepository.findByTenantIdAndTotalInstallmentsGreaterThan(tenantId, 1);
    }

    public List<Transaction> findByPaymentMethodAndDateRange(Long paymentMethodId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPaymentMethodIdAndDateBetween(paymentMethodId, startDate, endDate);
    }

    @Transactional(readOnly = false)
    public void delete(Transaction entity) {
        transactionRepository.delete(entity);
    }
}
