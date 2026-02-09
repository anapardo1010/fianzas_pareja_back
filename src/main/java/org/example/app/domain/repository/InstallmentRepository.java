package org.example.app.domain.repository;

import org.example.app.domain.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la gestión de Installments (cuotas MSI).
 */
@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    /**
     * Busca cuotas por transacción.
     */
    List<Installment> findByTransactionIdOrderByInstallmentNumberAsc(Long transactionId);

    /**
     * Busca cuotas por transacción (sin ordenamiento).
     */
    List<Installment> findByTransactionId(Long transactionId);

    /**
     * Busca cuotas pendientes por tenant en un rango de fechas.
     */
    List<Installment> findByTransactionTenantIdAndIsPaidFalseAndProjectedDateBetween(
        Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * Busca cuotas vencidas por tenant.
     */
    List<Installment> findByTransactionTenantIdAndIsPaidFalseAndProjectedDateBefore(
        Long tenantId, LocalDate currentDate);

    /**
     * Busca cuotas pendientes por método de pago en un rango de fechas.
     */
    List<Installment> findByTransactionPaymentMethodIdAndIsPaidFalseAndProjectedDateBetween(
        Long paymentMethodId, LocalDate startDate, LocalDate endDate);
}
