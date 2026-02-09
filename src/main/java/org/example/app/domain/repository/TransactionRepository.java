package org.example.app.domain.repository;

import org.example.app.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la gestión de Transactions por tenant.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Busca transacciones por tenant y rango de fechas.
     */
    List<Transaction> findByTenantIdAndDateBetweenOrderByDateDesc(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * Busca transacciones por tenant y rango de fechas (alias).
     */
    List<Transaction> findByTenantIdAndDateBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * Busca transacciones por tenant, usuario y fecha.
     */
    List<Transaction> findByTenantIdAndUserIdAndDateBetween(Long tenantId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Busca transacciones compartidas por tenant y fecha.
     */
    List<Transaction> findByTenantIdAndIsSharedTrueAndDateBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * Busca transacciones con MSI por tenant.
     */
    List<Transaction> findByTenantIdAndHasInstallmentsTrue(Long tenantId);

    /**
     * Busca transacciones con MSI por tenant (totalInstallments > valor).
     */
    List<Transaction> findByTenantIdAndTotalInstallmentsGreaterThan(Long tenantId, int minInstallments);

    /**
     * Busca transacciones por método de pago y rango de fechas.
     */
    List<Transaction> findByPaymentMethodIdAndDateBetween(Long paymentMethodId, LocalDate startDate, LocalDate endDate);
}
