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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Transactional
    public TransactionModel createTransaction(TransactionCreateModel createModel) {
        log.info("Creando transacción: {} por ${} para tenant: {}",
                createModel.getDescription(), createModel.getAmount(), createModel.getTenantId());

        Tenant tenant               = validateAndGetTenant(createModel.getTenantId());
        User user                   = validateAndGetUser(createModel.getUserId());
        Category category           = validateAndGetCategory(createModel.getCategoryId());
        PaymentMethod paymentMethod = validateAndGetPaymentMethod(createModel.getPaymentMethodId());

        validateUserBelongsToTenant(user, tenant);

        PaymentMethod destinationPaymentMethod = null;
        if ("TRANSFER".equalsIgnoreCase(createModel.getTransactionType())) {
            validateTransferLogic(createModel, paymentMethod);
            destinationPaymentMethod = validateAndGetPaymentMethod(createModel.getDestinationPaymentMethodId());
        }

        validateInstallmentLogic(createModel);

        Transaction transaction = createTransactionEntity(
                createModel, tenant, user, category, paymentMethod, destinationPaymentMethod);
        Transaction savedTransaction = transactionFacade.save(transaction);
        log.info("Transacción creada con ID: {}", savedTransaction.getId());

        if (Boolean.TRUE.equals(createModel.getHasInstallments())) {
            createInstallments(savedTransaction, createModel.getTotalInstallments());
            log.info("Creadas {} cuotas MSI para transacción: {}",
                    createModel.getTotalInstallments(), savedTransaction.getId());
        }

        if (Boolean.TRUE.equals(createModel.getIsShared()) && "EXPENSE".equals(createModel.getTransactionType())) {
            logProportionalCalculation(tenant, createModel.getAmount());
        }

        return TransactionModel.FN_ENTITY_TO_MODEL.apply(savedTransaction);
    }

    public List<TransactionModel> findByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Buscando transacciones para tenant: {} entre {} y {}", tenantId, startDate, endDate);
        return transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    public List<TransactionModel> findSharedByTenantAndDateRange(Long tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Buscando transacciones compartidas para tenant: {} entre {} y {}", tenantId, startDate, endDate);
        return transactionFacade.findSharedByTenantAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    public Optional<TransactionModel> findById(Long id) {
        log.debug("Buscando transacción por ID: {}", id);
        return transactionFacade.findById(id)
                .map(TransactionModel.FN_ENTITY_TO_MODEL);
    }

    public List<TransactionModel> findWithInstallmentsByTenant(Long tenantId) {
        log.debug("Buscando transacciones con MSI para tenant: {}", tenantId);
        return transactionFacade.findWithInstallmentsByTenant(tenantId)
                .stream()
                .map(TransactionModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionModel updateTransaction(Long id, TransactionCreateModel updateModel) {
        log.info("Actualizando transacción ID: {}", id);

        transactionFacade.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada: " + id));

        Tenant tenant               = validateAndGetTenant(updateModel.getTenantId());
        User user                   = validateAndGetUser(updateModel.getUserId());
        Category category           = validateAndGetCategory(updateModel.getCategoryId());
        PaymentMethod paymentMethod = validateAndGetPaymentMethod(updateModel.getPaymentMethodId());

        validateUserBelongsToTenant(user, tenant);

        PaymentMethod destinationPaymentMethod = null;
        if ("TRANSFER".equalsIgnoreCase(updateModel.getTransactionType())) {
            validateTransferLogic(updateModel, paymentMethod);
            destinationPaymentMethod = validateAndGetPaymentMethod(updateModel.getDestinationPaymentMethodId());
        }

        validateInstallmentLogic(updateModel);

        Transaction updatedTransaction = createTransactionEntity(
                updateModel, tenant, user, category, paymentMethod, destinationPaymentMethod);
        updatedTransaction.setId(id);

        Transaction result = transactionFacade.updateTransaction(updatedTransaction);
        return TransactionModel.FN_ENTITY_TO_MODEL.apply(result);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        log.info("Eliminando transacción ID: {}", id);
        transactionFacade.deleteTransaction(id);
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

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

    private void validateTransferLogic(TransactionCreateModel createModel, PaymentMethod sourcePaymentMethod) {
        if (createModel.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de la transferencia debe ser positivo");
        }
        if (createModel.getDestinationPaymentMethodId() == null) {
            throw new IllegalArgumentException("Debe especificar el método de pago destino para transferencias");
        }
        if (createModel.getPaymentMethodId().equals(createModel.getDestinationPaymentMethodId())) {
            throw new IllegalArgumentException("El método de pago origen y destino deben ser diferentes");
        }
        if (Boolean.TRUE.equals(createModel.getHasInstallments())) {
            throw new IllegalArgumentException("Las transferencias no pueden tener cuotas MSI");
        }
        log.info("Transferencia válida: ${} desde {} hacia método de pago ID {}",
                createModel.getAmount(),
                sourcePaymentMethod != null ? sourcePaymentMethod.getBankName() : "desconocido",
                createModel.getDestinationPaymentMethodId());
    }

    // =========================================================================
    // Construcción de entidad
    // =========================================================================

    private Transaction createTransactionEntity(TransactionCreateModel createModel, Tenant tenant,
                                                User user, Category category, PaymentMethod paymentMethod,
                                                PaymentMethod destinationPaymentMethod) {
        Transaction transaction = new Transaction(
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

        if (destinationPaymentMethod != null) {
            transaction.setDestinationPaymentMethod(destinationPaymentMethod);
        }

        return transaction;
    }

    // =========================================================================
    // Lógica MSI
    // =========================================================================

    /**
     * Crea las cuotas MSI automáticamente.
     *
     * Regla: la cuota 1 cae en la fecha de PAGO del ciclo donde ocurrió la compra.
     * Las siguientes cuotas caen en los ciclos subsecuentes.
     *
     * Ejemplo Ualá (cutDay=2, paymentDay=21, compra 22 enero):
     *   22 enero > corte 2 enero → siguiente corte = 2 febrero
     *   Cuota 1 → pago del ciclo 2/feb = 21/feb
     *   Cuota 2 → pago del ciclo 2/mar = 21/mar
     *
     * Ejemplo Didi Card (cutDay=21, paymentDay=5, compra 22 enero):
     *   22 enero > corte 21 enero → siguiente corte = 21 febrero
     *   Cuota 1 → pago del ciclo 21/feb = 5/mar
     *   Cuota 2 → pago del ciclo 21/mar = 5/abr
     */
    private void createInstallments(Transaction transaction, Integer totalInstallments) {
        List<Installment> installments = new ArrayList<>();

        BigDecimal installmentAmount = transaction.getAmount()
                .divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);

        PaymentMethod pm   = transaction.getPaymentMethod();
        Integer cutDay     = pm != null ? pm.getCutDay()     : null;
        Integer paymentDay = pm != null ? pm.getPaymentDay() : null;

        // Calcular el corte al que pertenece la compra (primer corte >= fecha de compra)
        LocalDate firstCutDate = resolveFirstCutDate(transaction.getDate(), cutDay);

        for (int i = 1; i <= totalInstallments; i++) {
            // Cuota i → cae en el ciclo (i-1) cortes después del primero
            LocalDate targetCutDate = firstCutDate.plusMonths(i - 1)
                    .withDayOfMonth(cutDay != null
                            ? Math.min(cutDay, firstCutDate.plusMonths(i - 1).lengthOfMonth())
                            : firstCutDate.getDayOfMonth());

            LocalDate projectedDate = calculatePaymentDateFromCut(targetCutDate, paymentDay);

            installments.add(new Installment(transaction, i, installmentAmount, projectedDate, false));
        }

        installmentFacade.saveAll(installments);
        log.debug("Creadas {} cuotas para transacción {} (primera fecha: {})",
                totalInstallments, transaction.getId(), installments.get(0).getProjectedDate());
    }

    /**
     * Encuentra el primer corte igual o posterior a la fecha de compra.
     * Si cutDay es null, usa la fecha de compra como base.
     */
    private LocalDate resolveFirstCutDate(LocalDate transactionDate, Integer cutDay) {
        if (cutDay == null) return transactionDate;

        // Corte de este mes
        LocalDate cutThisMonth = transactionDate.withDayOfMonth(
                Math.min(cutDay, transactionDate.lengthOfMonth()));

        // Si la compra ocurrió antes o en el día del corte → el corte es este mes
        // Si la compra ocurrió después del corte → el corte es el mes siguiente
        if (!transactionDate.isAfter(cutThisMonth)) {
            return cutThisMonth;
        } else {
            LocalDate nextMonth = transactionDate.plusMonths(1);
            return nextMonth.withDayOfMonth(Math.min(cutDay, nextMonth.lengthOfMonth()));
        }
    }

    /**
     * Calcula la fecha de pago a partir de la fecha de corte y el día de pago.
     * - paymentDay > cutDay  → mismo mes del corte
     * - paymentDay <= cutDay → mes siguiente al corte
     * - paymentDay null      → 20 días después del corte
     */
    private LocalDate calculatePaymentDateFromCut(LocalDate cutDate, Integer paymentDay) {
        if (paymentDay == null) return cutDate.plusDays(20);

        if (paymentDay > cutDate.getDayOfMonth()) {
            return cutDate.withDayOfMonth(Math.min(paymentDay, cutDate.lengthOfMonth()));
        } else {
            LocalDate next = cutDate.plusMonths(1);
            return next.withDayOfMonth(Math.min(paymentDay, next.lengthOfMonth()));
        }
    }

    // =========================================================================
    // Logging
    // =========================================================================

    private void logProportionalCalculation(Tenant tenant, BigDecimal amount) {
        List<User> tenantUsers = userFacade.findByTenantIdAndActive(tenant.getId());
        log.info("=== CÁLCULO PROPORCIONAL PARA GASTO COMPARTIDO ===");
        log.info("Monto total: ${}", amount);
        for (User user : tenantUsers) {
            BigDecimal userAmount = amount.multiply(user.getContributionPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            log.info("Usuario {}: {}% = ${}", user.getName(), user.getContributionPercentage(), userAmount);
        }
        log.info("=== FIN CÁLCULO PROPORCIONAL ===");
    }
}