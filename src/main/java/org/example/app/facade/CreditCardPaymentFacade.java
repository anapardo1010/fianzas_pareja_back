package org.example.app.facade;

import org.example.app.domain.entity.CreditCardPayment;
import org.example.app.domain.repository.CreditCardPaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class CreditCardPaymentFacade {

    private final CreditCardPaymentRepository repository;

    public CreditCardPaymentFacade(CreditCardPaymentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = false)
    public CreditCardPayment save(CreditCardPayment entity) {
        return repository.save(entity);
    }

    public Optional<CreditCardPayment> findById(Long id) {
        return repository.findById(id);
    }

    public List<CreditCardPayment> findByCardAndPeriod(Long creditCardId, String billingPeriodId) {
        return repository.findByCreditCardIdAndBillingPeriodId(creditCardId, billingPeriodId);
    }

    public List<CreditCardPayment> findByTenant(Long tenantId) {
        return repository.findByTenantIdOrderByPaidAtDesc(tenantId);
    }

    public List<CreditCardPayment> findByCard(Long creditCardId) {
        return repository.findByCreditCardIdOrderByPaidAtDesc(creditCardId);
    }

    public BigDecimal sumPaidByCardAndPeriod(Long creditCardId, String periodId) {
        return repository.sumPaidAmountByCardAndPeriod(creditCardId, periodId);
    }

    public boolean existsByCardAndPeriod(Long creditCardId, String billingPeriodId) {
        return repository.existsByCreditCardIdAndBillingPeriodId(creditCardId, billingPeriodId);
    }
}

