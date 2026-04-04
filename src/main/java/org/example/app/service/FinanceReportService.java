package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.User;
import org.example.app.domain.entity.Transaction;
import org.example.app.domain.entity.Installment;
import org.example.app.domain.entity.PaymentMethod;
import org.example.app.domain.entity.CreditCardPeriodPayment;
import org.example.app.domain.repository.CreditCardPeriodPaymentRepository;
import org.example.app.facade.TransactionFacade;
import org.example.app.facade.UserFacade;
import org.example.app.facade.InstallmentFacade;
import org.example.app.facade.PaymentMethodFacade;
import org.example.app.web.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para reportes financieros.
 *
 * Responsabilidades activas:
 *  1. Saldos de tarjetas de crédito por ciclo de corte  → getCreditCardBalances()
 *  2. Reparto proporcional por tarjeta                  → getCreditCardProportionalPayments()
 *  3. Marcar un periodo de tarjeta como pagado          → markPeriodAsPaid()
 *  4. Próximas cuotas MSI                               → getUpcomingInstallments()
 *  5. Reparto proporcional para débito/efectivo         → getNonCreditPaymentMethodProportionalPayments()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceReportService {

    private final TransactionFacade transactionFacade;
    private final UserFacade userFacade;
    private final InstallmentFacade installmentFacade;
    private final PaymentMethodFacade paymentMethodFacade;
    private final CreditCardPeriodPaymentRepository periodPaymentRepository;

    // =========================================================================
    // 1. Balance mensual
    // =========================================================================

    /**
     * Calcula ingresos vs gastos de un tenant en el rango dado.
     * Modo accrual: los gastos con tarjeta se cuentan cuando ocurren (no al pagar).
     * Modo cash:    solo cuenta salidas reales de efectivo/débito.
     */
    public MonthlyBalanceModel getMonthlyBalance(Long tenantId, YearMonth yearMonth, String mode) {
        log.info("Calculando balance mensual (modo={}) para tenant: {} en {}", mode, tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate   = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate);

        BigDecimal totalIncome   = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        int incomeCount = 0, expenseCount = 0;

        boolean accrual = mode == null || "accrual".equalsIgnoreCase(mode);

        for (Transaction tx : transactions) {
            String type = tx.getTransactionType();
            if ("TRANSFER".equalsIgnoreCase(type)) continue;

            if ("INCOME".equalsIgnoreCase(type)) {
                totalIncome = totalIncome.add(tx.getAmount());
                incomeCount++;
                continue;
            }

            if ("EXPENSE".equalsIgnoreCase(type) || "CREDIT_PAYMENT".equalsIgnoreCase(type)) {
                String accountType = tx.getPaymentMethod() != null ? tx.getPaymentMethod().getAccountType() : null;
                boolean isCreditPurchase = "CREDIT".equalsIgnoreCase(accountType) && "EXPENSE".equalsIgnoreCase(type);
                boolean isCreditPayment  = "CREDIT_PAYMENT".equalsIgnoreCase(type);

                if (accrual) {
                    if (isCreditPayment) continue; // evitar doble conteo
                    totalExpenses = totalExpenses.add(tx.getAmount());
                    expenseCount++;
                } else {
                    if (isCreditPurchase) continue; // se contabiliza al pagar
                    totalExpenses = totalExpenses.add(tx.getAmount());
                    expenseCount++;
                }
            }
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);
        log.info("Balance ({}): ingresos=${}, gastos=${}, neto=${}", accrual ? "accrual" : "cash", totalIncome, totalExpenses, netBalance);

        return new MonthlyBalanceModel(tenantId, yearMonth, totalIncome, totalExpenses, netBalance, incomeCount, expenseCount);
    }

    /**
     * Devuelve una lista de balances mensuales (últimos {@code months} meses) para el tenant.
     * El primer elemento de la lista será el mes más antiguo y el último el mes actual.
     * Mantiene el modo (accrual/cash) que usa la lógica mensual.
     */
    public List<MonthlyBalanceModel> getMonthlyBalances(Long tenantId, int months, String mode) {
        log.info("Calculando balances mensuales para tenant: {} últimos {} meses (modo={})", tenantId, months, mode);

        if (months <= 0) months = 6;

        List<MonthlyBalanceModel> balances = new ArrayList<>();
        YearMonth current = YearMonth.now();

        // Construir desde el mes más antiguo hasta el actual
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            MonthlyBalanceModel m = getMonthlyBalance(tenantId, ym, mode);
            balances.add(m);
        }

        return balances;
    }

    /**
     * Conveniencia: devuelve los últimos 6 meses en modo 'accrual'.
     */
    public List<MonthlyBalanceModel> getMonthlyBalances(Long tenantId) {
        return getMonthlyBalances(tenantId, 6, "accrual");
    }

    // =========================================================================
    // 2. Balance por método de pago
    // =========================================================================

    /**
     * Calcula ingresos, gastos y transferencias de cada método de pago en el mes.
     * Usado en el dashboard para saber si el usuario ya tiene métodos configurados.
     */
    public List<PaymentMethodBalanceModel> getBalanceByPaymentMethod(Long tenantId, YearMonth yearMonth) {
        log.info("Calculando balance por método de pago para tenant: {} en {}", tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate   = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate);
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);
        Map<Long, PaymentMethodBalanceModel> balanceMap = new HashMap<>();

        // Inicializar con todos los métodos de pago del tenant
        for (User user : users) {
            for (PaymentMethod pm : paymentMethodFacade.findByUserIdAndActive(user.getId())) {
                balanceMap.put(pm.getId(), emptyBalance(pm));
            }
        }

        for (Transaction tx : transactions) {
            String type    = tx.getTransactionType();
            BigDecimal amt = tx.getAmount();
            Long pmId      = tx.getPaymentMethod() != null ? tx.getPaymentMethod().getId() : null;
            Long destPmId  = tx.getDestinationPaymentMethod() != null ? tx.getDestinationPaymentMethod().getId() : null;

            if (pmId != null && !balanceMap.containsKey(pmId)) {
                balanceMap.put(pmId, emptyBalance(tx.getPaymentMethod()));
            }

            if ("INCOME".equalsIgnoreCase(type) && pmId != null) {
                PaymentMethodBalanceModel m = balanceMap.get(pmId);
                m.setTotalIncome(m.getTotalIncome().add(amt));
                m.setBalance(m.getBalance().add(amt));
                m.setTransactionCount(m.getTransactionCount() + 1);

            } else if ("EXPENSE".equalsIgnoreCase(type) && pmId != null) {
                PaymentMethodBalanceModel m = balanceMap.get(pmId);
                m.setTotalExpenses(m.getTotalExpenses().add(amt));
                m.setBalance(m.getBalance().subtract(amt));
                m.setTransactionCount(m.getTransactionCount() + 1);

            } else if ("CREDIT_PAYMENT".equalsIgnoreCase(type) && pmId != null) {
                PaymentMethodBalanceModel m = balanceMap.get(pmId);
                m.setTotalExpenses(m.getTotalExpenses().add(amt));
                m.setBalance(m.getBalance().subtract(amt));
                m.setTransactionCount(m.getTransactionCount() + 1);

            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                if (pmId != null) {
                    PaymentMethodBalanceModel src = balanceMap.get(pmId);
                    src.setTransfersOut(src.getTransfersOut().add(amt));
                    src.setBalance(src.getBalance().subtract(amt));
                    src.setTransactionCount(src.getTransactionCount() + 1);
                }
                if (destPmId != null) {
                    balanceMap.computeIfAbsent(destPmId, id -> emptyBalance(tx.getDestinationPaymentMethod()));
                    PaymentMethodBalanceModel dst = balanceMap.get(destPmId);
                    dst.setTransfersIn(dst.getTransfersIn().add(amt));
                    dst.setBalance(dst.getBalance().add(amt));
                }
            }
        }

        List<PaymentMethodBalanceModel> result = new ArrayList<>(balanceMap.values());
        log.info("Balance calculado para {} métodos de pago", result.size());
        return result;
    }

    // =========================================================================
    // 1. Saldos de tarjetas de crédito
    // =========================================================================

    /**
     * Calcula el saldo del ciclo actual de cada tarjeta de crédito del tenant.
     * Considera fecha de corte, fecha de pago, transacciones y cuotas MSI.
     * Si el periodo más reciente ya está pagado, avanza al siguiente ciclo.
     */
    public List<CreditCardBalanceModel> getCreditCardBalances(Long tenantId) {
        log.info("Calculando saldos de tarjetas de crédito para tenant: {}", tenantId);

        LocalDate today = LocalDate.now();
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);
        List<CreditCardBalanceModel> balances = new ArrayList<>();

        for (User user : users) {
            List<PaymentMethod> paymentMethods = paymentMethodFacade.findByUserIdAndActive(user.getId());

            for (PaymentMethod pm : paymentMethods) {

                if (!"CREDIT".equals(pm.getAccountType()) || pm.getCutDay() == null) {
                    log.debug("Saltando PM {}: no es tarjeta de crédito o no tiene día de corte", pm.getId());
                    continue;
                }

                LocalDate lastCutDate     = calculateLastCutDate(today, pm.getCutDay());
                LocalDate previousCutDate = lastCutDate.minusMonths(1);
                LocalDate nextCutDate     = lastCutDate.plusMonths(1);

                String lastPeriodId = previousCutDate.plusDays(1) + "_" + lastCutDate;
                boolean lastPeriodPaid = periodPaymentRepository
                        .existsByPaymentMethodIdAndPeriodId(pm.getId(), lastPeriodId);

                LocalDate rangeStart, rangeEnd, displayCutDate, currentPaymentDate;
                String periodId;
                boolean isPaid;

                if (lastPeriodPaid) {
                    rangeStart         = lastCutDate.plusDays(1);
                    rangeEnd           = nextCutDate;
                    displayCutDate     = nextCutDate;
                    currentPaymentDate = calculatePaymentDate(nextCutDate, pm.getPaymentDay());
                    periodId           = rangeStart + "_" + rangeEnd;
                    isPaid             = periodPaymentRepository
                            .existsByPaymentMethodIdAndPeriodId(pm.getId(), periodId);
                } else {
                    rangeStart         = previousCutDate.plusDays(1);
                    rangeEnd           = lastCutDate;
                    displayCutDate     = lastCutDate;
                    currentPaymentDate = calculatePaymentDate(lastCutDate, pm.getPaymentDay());
                    periodId           = lastPeriodId;
                    isPaid             = false;
                }

                log.info("Tarjeta {}: today={}, displayCutDate={}, nextCutDate={}, currentPaymentDate={}, daysUntilCut={}, daysUntilPayment={}, isPaid={}",
                        pm.getBankName(), today, displayCutDate, nextCutDate, currentPaymentDate,
                        ChronoUnit.DAYS.between(today, displayCutDate),
                        ChronoUnit.DAYS.between(today, currentPaymentDate), isPaid);

                List<Transaction> transactions = transactionFacade
                        .findByPaymentMethodAndDateRange(pm.getId(), rangeStart, rangeEnd);

                // Solo sumar transacciones directas (sin MSI). Las de MSI se cuentan vía cuotas.
                BigDecimal currentBalance = transactions.stream()
                        .filter(tx -> !Boolean.TRUE.equals(tx.getHasInstallments()))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Installment> installments = installmentFacade
                        .findPendingByPaymentMethodAndDateRange(pm.getId(), rangeStart, rangeEnd);

                BigDecimal pendingInstallments = installments.stream()
                        .map(Installment::getInstallmentAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalDue = currentBalance.add(pendingInstallments);

                String paymentStatus;
                if (isPaid) {
                    paymentStatus = "PAID";
                } else if (today.isAfter(currentPaymentDate)) {
                    paymentStatus = "OVERDUE";
                } else {
                    paymentStatus = "PENDING";
                }

                String status;
                if (today.isBefore(displayCutDate)) {
                    status = "PENDING_CUT";
                } else if (!today.isAfter(currentPaymentDate)) {
                    status = "PENDING_PAYMENT";
                } else {
                    status = "OVERDUE";
                }

                int daysUntilCut     = (int) ChronoUnit.DAYS.between(today, displayCutDate);
                int daysUntilPayment = (int) ChronoUnit.DAYS.between(today, currentPaymentDate);

                log.info("Tarjeta {}: Saldo directo ${}, Cuotas MSI ${}, Total ${}, Status: {}, PaymentStatus: {}, Pagado: {}, PeriodId: {}",
                        pm.getBankName(), currentBalance, pendingInstallments, totalDue, status, paymentStatus, isPaid, periodId);

                balances.add(new CreditCardBalanceModel(
                        pm.getId(),
                        pm.getUser() != null ? pm.getUser().getId() : null,
                        pm.getAlias(),
                        pm.getBankName(),
                        pm.getAccountType(),
                        pm.getCutDay(),
                        pm.getPaymentDay(),
                        displayCutDate,
                        currentPaymentDate,
                        nextCutDate,
                        currentBalance,
                        pendingInstallments,
                        totalDue,
                        transactions.size(),
                        installments.size(),
                        status,
                        daysUntilCut,
                        daysUntilPayment,
                        paymentStatus,
                        isPaid,
                        periodId
                ));

                log.info("Tarjeta {}: total=${}, status={}, paymentStatus={}, periodId={}",
                        pm.getBankName(), totalDue, status, paymentStatus, periodId);
            }
        }

        return balances;
    }

    // =========================================================================
    // 2. Reparto proporcional por tarjeta
    // =========================================================================

    /**
     * Para cada tarjeta, calcula cuánto debe pagar cada usuario:
     * - Transacciones compartidas → se dividen según contributionPercentage.
     * - Transacciones individuales → 100% al usuario que las realizó.
     * - Cuotas MSI siguen la misma regla que la transacción original.
     */
    public List<CreditCardProportionalPaymentModel> getCreditCardProportionalPayments(Long tenantId) {
        log.info("Calculando pagos proporcionales de tarjetas para tenant: {}", tenantId);

        List<User> users = userFacade.findByTenantIdAndActive(tenantId);
        List<CreditCardProportionalPaymentModel> result = new ArrayList<>();

        for (CreditCardBalanceModel card : getCreditCardBalances(tenantId)) {
            String[] parts       = card.getPeriodId().split("_");
            LocalDate rangeStart = LocalDate.parse(parts[0]);
            LocalDate rangeEnd   = LocalDate.parse(parts[1]);

            List<Transaction> transactions = transactionFacade
                    .findByPaymentMethodAndDateRange(card.getPaymentMethodId(), rangeStart, rangeEnd);

            List<Installment> installments = installmentFacade
                    .findPendingByPaymentMethodAndDateRange(card.getPaymentMethodId(), rangeStart, rangeEnd);

            Map<Long, BigDecimal> txSums          = buildZeroMap(users);
            Map<Long, BigDecimal> installmentSums = buildZeroMap(users);

            // Solo distribuir transacciones directas (sin MSI).
            // Las que tienen MSI se distribuyen por sus cuotas individuales abajo.
            for (Transaction tx : transactions) {
                if (Boolean.TRUE.equals(tx.getHasInstallments())) {
                    log.debug("Tarjeta {}: omitiendo tx MSI id={} '{}' ${} (se contabiliza por cuotas)",
                            card.getBankName(), tx.getId(), tx.getDescription(), tx.getAmount());
                    continue;
                }
                log.debug("Tarjeta {}: distribuyendo tx id={} '{}' ${} shared={}",
                        card.getBankName(), tx.getId(), tx.getDescription(), tx.getAmount(), tx.getIsShared());
                distributeAmount(tx.getAmount(), tx.getIsShared(), tx.getUser().getId(), users, txSums);
            }

            for (Installment inst : installments) {
                log.debug("Tarjeta {}: distribuyendo cuota id={} txId={} '{}' ${} shared={}",
                        card.getBankName(), inst.getId(), inst.getTransaction().getId(),
                        inst.getTransaction().getDescription(), inst.getInstallmentAmount(),
                        inst.getTransaction().getIsShared());
                distributeAmount(
                        inst.getInstallmentAmount(),
                        inst.getTransaction().getIsShared(),
                        inst.getTransaction().getUser().getId(),
                        users,
                        installmentSums
                );
            }

            List<UserPaymentShare> userShares = users.stream()
                    .map(u -> {
                        BigDecimal txPart   = txSums.get(u.getId());
                        BigDecimal instPart = installmentSums.get(u.getId());
                        BigDecimal total    = txPart.add(instPart);
                        BigDecimal pct      = u.getContributionPercentage() != null
                                ? u.getContributionPercentage() : BigDecimal.valueOf(100);
                        log.info("Tarjeta {}: Usuario {} debe pagar ${} ({}% de su deuda: transacciones ${} + cuotas ${})",
                                card.getBankName(), u.getName(), total, pct, txPart, instPart);
                        return new UserPaymentShare(u.getId(), u.getName(), pct, total);
                    })
                    .collect(Collectors.toList());

            result.add(new CreditCardProportionalPaymentModel(
                    card.getPaymentMethodId(),
                    card.getUserId(),
                    card.getAlias(),
                    card.getBankName(),
                    card.getCurrentCutDate(),
                    card.getCurrentPaymentDate(),
                    card.getCurrentBalance(),
                    card.getPendingInstallments(),
                    card.getTotalDue(),
                    card.getStatus(),
                    card.getPaymentStatus(),
                    card.getPeriodId(),
                    userShares
            ));
        }

        log.info("Calculados pagos proporcionales para {} tarjetas", result.size());
        return result;
    }

    // =========================================================================
    // 3. Marcar periodo como pagado
    // =========================================================================

    /**
     * Persiste un registro que indica que el periodo dado de una tarjeta fue pagado.
     * Si ya existe el registro, no hace nada.
     */
    public void markPeriodAsPaid(Long paymentMethodId, String periodId) {
        log.info("Marcando periodo {} como pagado para tarjeta {}", periodId, paymentMethodId);

        if (periodPaymentRepository.existsByPaymentMethodIdAndPeriodId(paymentMethodId, periodId)) {
            log.warn("El periodo {} ya estaba marcado como pagado", periodId);
            return;
        }

        String[] dates      = periodId.split("_");
        LocalDate periodStart = LocalDate.parse(dates[0]);
        LocalDate periodEnd   = LocalDate.parse(dates[1]);

        PaymentMethod pm = paymentMethodFacade.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Método de pago no encontrado: " + paymentMethodId));

        periodPaymentRepository.save(new CreditCardPeriodPayment(pm, periodId, periodStart, periodEnd));
        log.info("Periodo {} marcado como pagado exitosamente", periodId);
    }

    // =========================================================================
    // 4. Próximas cuotas MSI
    // =========================================================================

    /**
     * Devuelve las cuotas MSI pendientes que vencen en los próximos {@code nextMonths} meses.
     */
    public List<UpcomingInstallmentModel> getUpcomingInstallments(Long tenantId, int nextMonths) {
        log.info("Obteniendo cuotas MSI para tenant: {} en los próximos {} meses", tenantId, nextMonths);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = startDate.plusMonths(nextMonths);

        return installmentFacade
                .findPendingByTenantAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(inst -> new UpcomingInstallmentModel(
                        inst.getId(),
                        inst.getTransaction().getId(),
                        inst.getTransaction().getDescription(),
                        inst.getInstallmentNumber(),
                        inst.getTransaction().getTotalInstallments(),
                        inst.getInstallmentAmount(),
                        inst.getProjectedDate(),
                        inst.getTransaction().getPaymentMethod().getBankName()
                ))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 5. Reparto proporcional para débito / efectivo
    // =========================================================================

    /**
     * Para cada método de pago no crediticio (DEBIT, CASH, etc.) calcula cuánto
     * debe asumir cada usuario en el rango indicado.
     * Si {@code startDate} o {@code endDate} son null, usa el mes actual.
     */
    public List<PaymentMethodProportionalPaymentModel> getNonCreditPaymentMethodProportionalPayments(
            Long tenantId, LocalDate startDate, LocalDate endDate) {

        // Rango por defecto: mes actual
        if (startDate == null || endDate == null) {
            YearMonth current = YearMonth.now();
            startDate = current.atDay(1);
            endDate   = current.atEndOfMonth();
        }

        log.info("Calculando pagos proporcionales (no-credit) para tenant: {} — {} a {}", tenantId, startDate, endDate);

        List<User> users  = userFacade.findByTenantIdAndActive(tenantId);
        List<PaymentMethodProportionalPaymentModel> result = new ArrayList<>();

        for (User user : users) {
            for (PaymentMethod pm : paymentMethodFacade.findByUserIdAndActive(user.getId())) {

                if ("CREDIT".equalsIgnoreCase(pm.getAccountType())) continue;

                List<Transaction> transactions = transactionFacade
                        .findByPaymentMethodAndDateRange(pm.getId(), startDate, endDate);

                Map<Long, BigDecimal> userSums = buildZeroMap(users);
                BigDecimal totalExpenses = BigDecimal.ZERO;
                int txCount = 0;

                for (Transaction tx : transactions) {
                    if (!"EXPENSE".equalsIgnoreCase(tx.getTransactionType())) continue;
                    if (Boolean.TRUE.equals(tx.getHasInstallments())) continue;

                    totalExpenses = totalExpenses.add(tx.getAmount());
                    txCount++;
                    distributeAmount(tx.getAmount(), tx.getIsShared(),
                            tx.getUser() != null ? tx.getUser().getId() : null, users, userSums);
                }

                List<UserPaymentShare> userShares = users.stream()
                        .map(u -> new UserPaymentShare(
                                u.getId(),
                                u.getName(),
                                u.getContributionPercentage() != null ? u.getContributionPercentage() : BigDecimal.valueOf(100),
                                userSums.getOrDefault(u.getId(), BigDecimal.ZERO)
                        ))
                        .collect(Collectors.toList());

                result.add(new PaymentMethodProportionalPaymentModel(
                        pm.getId(),
                        pm.getUser() != null ? pm.getUser().getId() : null,
                        pm.getAlias(),
                        pm.getBankName(),
                        pm.getAccountType(),
                        startDate,
                        endDate,
                        totalExpenses,
                        txCount,
                        userShares
                ));
            }
        }

        log.info("Calculados pagos proporcionales (no-credit) para {} métodos", result.size());
        return result;
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private PaymentMethodBalanceModel emptyBalance(PaymentMethod pm) {
        return new PaymentMethodBalanceModel(pm.getId(), pm.getBankName(), pm.getAlias(),
                pm.getAccountType(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    /** Inicializa un mapa userId → ZERO para todos los usuarios. */
    private Map<Long, BigDecimal> buildZeroMap(List<User> users) {
        Map<Long, BigDecimal> map = new HashMap<>();
        users.forEach(u -> map.put(u.getId(), BigDecimal.ZERO));
        return map;
    }

    /**
     * Distribuye {@code amount} en el mapa:
     * - Si es compartida, reparte proporcionalmente según contributionPercentage.
     * - Si no lo es, asigna el total al usuario con id {@code ownerId}.
     */
    private void distributeAmount(BigDecimal amount, Boolean isShared, Long ownerId,
                                  List<User> users, Map<Long, BigDecimal> sums) {
        if (Boolean.TRUE.equals(isShared)) {
            for (User u : users) {
                BigDecimal share = amount
                        .multiply(u.getContributionPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                sums.merge(u.getId(), share, BigDecimal::add);
            }
        } else if (ownerId != null && sums.containsKey(ownerId)) {
            sums.merge(ownerId, amount, BigDecimal::add);
        }
    }

    /**
     * Fecha del último corte que ya ocurrió (igual o antes de hoy).
     */
    private LocalDate calculateLastCutDate(LocalDate today, Integer cutDay) {
        LocalDate cutDate = today.withDayOfMonth(Math.min(cutDay, today.lengthOfMonth()));
        if (today.isBefore(cutDate)) {
            cutDate = cutDate.minusMonths(1)
                    .withDayOfMonth(Math.min(cutDay, cutDate.minusMonths(1).lengthOfMonth()));
        }
        return cutDate;
    }

    /**
     * Fecha de pago a partir de la fecha de corte y el día de pago configurado.
     * - Si paymentDay > cutDay  → mismo mes del corte.
     * - Si paymentDay <= cutDay → mes siguiente al corte.
     * - Si paymentDay es null   → 20 días después del corte.
     */
    private LocalDate calculatePaymentDate(LocalDate cutDate, Integer paymentDay) {
        if (paymentDay == null) return cutDate.plusDays(20);

        if (paymentDay > cutDate.getDayOfMonth()) {
            return cutDate.withDayOfMonth(Math.min(paymentDay, cutDate.lengthOfMonth()));
        } else {
            LocalDate next = cutDate.plusMonths(1);
            return next.withDayOfMonth(Math.min(paymentDay, next.lengthOfMonth()));
        }
    }
}
