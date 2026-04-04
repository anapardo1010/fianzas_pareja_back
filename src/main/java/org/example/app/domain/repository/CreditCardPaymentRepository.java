package org.example.app.domain.repository;

import org.example.app.domain.entity.CreditCardPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditCardPaymentRepository extends JpaRepository<CreditCardPayment, Long> {

    /** Todos los pagos de una tarjeta en un periodo específico. */
    List<CreditCardPayment> findByCreditCardIdAndBillingPeriodId(Long creditCardId, String billingPeriodId);

    /** Todos los pagos de un tenant (para historial). */
    List<CreditCardPayment> findByTenantIdOrderByPaidAtDesc(Long tenantId);

    /** Todos los pagos de una tarjeta en todos los periodos (historial por tarjeta). */
    List<CreditCardPayment> findByCreditCardIdOrderByPaidAtDesc(Long creditCardId);

    /** Suma total pagado para una tarjeta en un periodo (para saber si está cubierto). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CreditCardPayment p " +
           "WHERE p.creditCard.id = :creditCardId AND p.billingPeriodId = :periodId")
    java.math.BigDecimal sumPaidAmountByCardAndPeriod(
            @Param("creditCardId") Long creditCardId,
            @Param("periodId") String periodId);

    /** Verifica si ya existe algún pago para esa tarjeta y periodo. */
    boolean existsByCreditCardIdAndBillingPeriodId(Long creditCardId, String billingPeriodId);
}

