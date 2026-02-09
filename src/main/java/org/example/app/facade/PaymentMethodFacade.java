package org.example.app.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import org.example.app.domain.entity.PaymentMethod;
import org.example.app.domain.repository.PaymentMethodRepository;

@Component
@Transactional(readOnly = true)
public class PaymentMethodFacade {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodFacade(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public Page<PaymentMethod> findPage(Pageable pageable) {
        return paymentMethodRepository.findAll(pageable);
    }

    public Page<PaymentMethod> findAll(Pageable pageable) {
        return paymentMethodRepository.findAll(pageable);
    }

    public Optional<PaymentMethod> findById(Long id) {
        return paymentMethodRepository.findById(id);
    }

    @Transactional(readOnly = false)
    public PaymentMethod save(PaymentMethod entity) {
        return paymentMethodRepository.save(entity);
    }

    public List<PaymentMethod> findByUserIdAndActive(Long userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public List<PaymentMethod> findByTenantAndActive(Long tenantId) {
        return paymentMethodRepository.findByUserTenantIdAndIsActiveTrue(tenantId);
    }

    public Page<PaymentMethod> findByTenantAndActive(Long tenantId, Pageable pageable) {
        return paymentMethodRepository.findByUserTenantIdAndIsActiveTrue(tenantId, pageable);
    }

    @Transactional(readOnly = false)
    public void delete(PaymentMethod entity) {
        paymentMethodRepository.delete(entity);
    }
}
