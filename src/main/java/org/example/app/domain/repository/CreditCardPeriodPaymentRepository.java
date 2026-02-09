package org.example.app.domain.repository;

import org.example.app.domain.entity.CreditCardPeriodPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar los periodos de tarjeta de crédito pagados.
 */
@Repository
public interface CreditCardPeriodPaymentRepository extends JpaRepository<CreditCardPeriodPayment, Long> {

    /**
     * Verifica si un periodo específico ya fue marcado como pagado.
     */
    boolean existsByPaymentMethodIdAndPeriodId(Long paymentMethodId, String periodId);

    /**
     * Busca un registro de pago por método de pago y periodo.
     */
    Optional<CreditCardPeriodPayment> findByPaymentMethodIdAndPeriodId(Long paymentMethodId, String periodId);
}

