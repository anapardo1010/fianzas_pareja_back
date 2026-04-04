package org.example.app.domain.repository;

import org.example.app.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Elimina una transacción por su ID.
     */
    void deleteById(Long id);

    /**
     * Filtro flexible por tenant con parámetros opcionales.
     * Se usan casteos explícitos para compatibilidad con PostgreSQL.
     */
    @Query(value = """
            SELECT * FROM transaction
            WHERE id_tenant = :tenantId
            AND (CAST(:startDate AS date) IS NULL OR date >= CAST(:startDate AS date))
            AND (CAST(:endDate AS date) IS NULL OR date <= CAST(:endDate AS date))
            AND (CAST(:transactionType AS text) IS NULL OR LOWER(transaction_type) = LOWER(CAST(:transactionType AS text)))
            AND (CAST(:paymentMethodId AS bigint) IS NULL OR id_payment_method = CAST(:paymentMethodId AS bigint))
            ORDER BY date DESC, created_at DESC
            """, nativeQuery = true)
    List<Transaction> findByFilters(
            @Param("tenantId")        Long tenantId,
            @Param("startDate")       LocalDate startDate,
            @Param("endDate")         LocalDate endDate,
            @Param("transactionType") String transactionType,
            @Param("paymentMethodId") Long paymentMethodId);
}
