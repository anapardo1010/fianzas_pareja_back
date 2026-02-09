package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.*;
import org.example.app.facade.*;
import org.example.app.web.model.TransactionCreateModel;
import org.example.app.web.model.TransactionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de Transactions (operaciones core).
 * Implementa la lógica compleja de gastos compartidos y MSI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionFacade transactionFacade;
    private final InstallmentFacade installmentFacade;
    private final TenantFacade tenantFacade;
    private final UserFacade userFacade;
    private final CategoryFacade categoryFacade;
    private final PaymentMethodFacade paymentMethodFacade;

    /**
     * Crea una nueva transacción con lógica de gastos compartidos y MSI.
     * Esta es la operación core del sistema.
     */
    @Transactional
    public TransactionModel createTransaction(TransactionCreateModel createModel) {
        log.info("Creando transacción: {} por $${} para tenant: {}",
                createModel.getDescription(), createModel.getAmount(), createModel.getTenantId());

        // Validar entidades relacionadas
        Tenant tenant = validateAndGetTenant(createModel.getTenantId());
        User user = validateAndGetUser(createModel.getUserId());
        Category category = validateAndGetCategory(createModel.getCategoryId());
        PaymentMethod paymentMethod = validateAndGetPaymentMethod(createModel.getPaymentMethodId());

        // Validar que el usuario pertenece al tenant
        validateUserBelongsToTenant(user, tenant);

        // Validar lógica de MSI
        validateInstallmentLogic(createModel);

        // Crear la transacción principal
        Transaction transaction = createTransactionEntity(createModel, tenant, user, category, paymentMethod);
        Transaction savedTransaction = transactionFacade.save(transaction);
        log.info("Transacción creada con ID: {}", savedTransaction.getId());

        // Si tiene MSI, crear las cuotas automáticamente
        if (Boolean.TRUE.equals(createModel.getHasInstallments())) {
            createInstallments(savedTransaction, createModel.getTotalInstallments());
            log.info("Creadas {} cuotas MSI para transacción: {}",
                    createModel.getTotalInstallments(), savedTransaction.getId());
        }

        // Si es gasto compartido, loggear información de cálculo proporcional
        if (Boolean.TRUE.equals(createModel.getIsShared()) && "EXPENSE".equals(createModel.getTransactionType())) {
            logProportionalCalculation(tenant, createModel.getAmount());
        }

        return TransactionModel.FN_ENTITY_TO_MODEL.apply(savedTransaction);
    }

    /**
     * Busca transacciones por tenant y rango de fechas.
     */
    public List<TransactionModel> findByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Buscando transacciones para tenant: {} entre {} y {}", tenantId, startDate, endDate);
        return transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Busca transacciones compartidas por tenant y fecha.
     */
    public List<TransactionModel> findSharedByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Buscando transacciones compartidas para tenant: {} entre {} y {}", tenantId, startDate, endDate);
        return transactionFacade.findSharedByTenantAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Busca una transacción por ID.
     */
    public Optional<TransactionModel> findById(Long id) {
        log.debug("Buscando transacción por ID: {}", id);
        return transactionFacade.findById(id)
                .map(TransactionModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca transacciones con MSI por tenant.
     */
    public List<TransactionModel> findWithInstallmentsByTenant(Long tenantId) {
        log.debug("Buscando transacciones con MSI para tenant: {}", tenantId);
        return transactionFacade.findWithInstallmentsByTenant(tenantId)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    // Métodos privados de validación y lógica de negocio

    private Tenant validateAndGetTenant(Long tenantId) {
        return tenantFacade.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));
    }

    private User validateAndGetUser(Long userId) {
        return userFacade.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    private Category validateAndGetCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryFacade.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + categoryId));
    }

    private PaymentMethod validateAndGetPaymentMethod(Long paymentMethodId) {
        if (paymentMethodId == null) return null;
        return paymentMethodFacade.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado: " + paymentMethodId));
    }

    private void validateUserBelongsToTenant(User user, Tenant tenant) {
        if (!user.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("El usuario no pertenece al tenant especificado");
        }
    }

    private void validateInstallmentLogic(TransactionCreateModel createModel) {
        if (Boolean.TRUE.equals(createModel.getHasInstallments())) {
            if (createModel.getTotalInstallments() == null || createModel.getTotalInstallments() < 2) {
                throw new IllegalArgumentException("Las cuotas MSI deben ser mínimo 2");
            }
            if (createModel.getTotalInstallments() > 36) {
                throw new IllegalArgumentException("Las cuotas MSI no pueden exceder 36 meses");
            }
            if (!"EXPENSE".equals(createModel.getTransactionType())) {
                throw new IllegalArgumentException("Solo los gastos pueden tener MSI");
            }
        }
    }

    private Transaction createTransactionEntity(TransactionCreateModel createModel, Tenant tenant,
                                               User user, Category category, PaymentMethod paymentMethod) {
        return new Transaction(
            tenant,
            user,
            category,
            paymentMethod,
            createModel.getDescription(),
            createModel.getAmount(),
            createModel.getDate(),
            createModel.getIsShared(),
            createModel.getTransactionType(),
            createModel.getHasInstallments(),
            createModel.getTotalInstallments()
        );
    }

    /**
     * Crea las cuotas MSI automáticamente basadas en el día de corte de la tarjeta.
     */
    private void createInstallments(Transaction transaction, Integer totalInstallments) {
        List<Installment> installments = new ArrayList<>();
        BigDecimal installmentAmount = transaction.getAmount().divide(
            BigDecimal.valueOf(totalInstallments), 2, BigDecimal.ROUND_HALF_UP);

        // Obtener día de corte de la tarjeta (si aplica)
        Integer cutDay = transaction.getPaymentMethod() != null ?
            transaction.getPaymentMethod().getCutDay() : null;

        for (int i = 1; i <= totalInstallments; i++) {
            LocalDate projectedDate = calculateInstallmentDate(transaction.getDate(), i, cutDay);

            Installment installment = new Installment(
                transaction,
                i,
                installmentAmount,
                projectedDate,
                false // isPaid = false por defecto
            );
            installments.add(installment);
        }

        installmentFacade.saveAll(installments);
        log.debug("Creadas {} cuotas para transacción {}", totalInstallments, transaction.getId());
    }

    /**
     * Calcula la fecha proyectada de una cuota considerando el día de corte.
     */
    private LocalDate calculateInstallmentDate(LocalDate transactionDate, int installmentNumber, Integer cutDay) {
        if (cutDay == null) {
            // Si no hay día de corte, simplemente sumar meses
            return transactionDate.plusMonths(installmentNumber);
        }

        // Lógica más compleja considerando día de corte de tarjeta
        LocalDate baseDate = transactionDate.plusMonths(installmentNumber);

        // Si el día del mes es mayor al día de corte, se va al siguiente mes
        if (transactionDate.getDayOfMonth() > cutDay) {
            baseDate = baseDate.plusMonths(1);
        }

        // Establecer el día de corte como día de proyección
        try {
            return baseDate.withDayOfMonth(cutDay);
        } catch (Exception e) {
            // Si el día no existe en ese mes (ej: 31 en febrero), usar último día del mes
            return baseDate.withDayOfMonth(baseDate.lengthOfMonth());
        }
    }

    /**
     * Loggea información sobre el cálculo proporcional para gastos compartidos.
     */
    private void logProportionalCalculation(Tenant tenant, BigDecimal amount) {
        List<User> tenantUsers = userFacade.findByTenantIdAndActive(tenant.getId());

        log.info("=== CÁLCULO PROPORCIONAL PARA GASTO COMPARTIDO ===");
        log.info("Monto total: ${}", amount);

        for (User user : tenantUsers) {
            BigDecimal userAmount = amount.multiply(user.getContributionPercentage())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            log.info("Usuario {}: {}% = ${}", user.getName(), user.getContributionPercentage(), userAmount);
        }
        log.info("=== FIN CÁLCULO PROPORCIONAL ===");
    }
}
