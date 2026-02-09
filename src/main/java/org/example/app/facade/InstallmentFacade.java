package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.Installment;
import org.example.app.domain.repository.InstallmentRepository;

@Component
@Transactional(readOnly = true)
public class InstallmentFacade {

    private final InstallmentRepository installmentRepository;

    public InstallmentFacade(InstallmentRepository installmentRepository) {
        this.installmentRepository = installmentRepository;
    }

    public Page<Installment> findPage(Pageable pageable) {
        return installmentRepository.findAll(pageable);
    }

    public Optional<Installment> findById(Long id) {
        return installmentRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public Installment save(Installment entity) {
        return installmentRepository.save(entity);
    }

    @Transactional(readOnly = false)
    public List<Installment> saveAll(List<Installment> installments) {
        return installmentRepository.saveAll(installments);
    }

    public List<Installment> findPendingByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return installmentRepository.findByTransactionTenantIdAndIsPaidFalseAndProjectedDateBetween(
            tenantId, startDate, endDate);
    }

    public List<Installment> findOverdueByTenant(Long tenantId, LocalDate currentDate) {
        return installmentRepository.findByTransactionTenantIdAndIsPaidFalseAndProjectedDateBefore(
            tenantId, currentDate);
    }

    public List<Installment> findByTransaction(Long transactionId) {
        return installmentRepository.findByTransactionId(transactionId);
    }

    public List<Installment> findPendingByPaymentMethodAndDateRange(Long paymentMethodId, LocalDate startDate, LocalDate endDate) {
        return installmentRepository.findByTransactionPaymentMethodIdAndIsPaidFalseAndProjectedDateBetween(
            paymentMethodId, startDate, endDate);
    }

    @Transactional(readOnly = false)
    public void delete(Installment entity) {
        installmentRepository.delete(entity);
    }
}
