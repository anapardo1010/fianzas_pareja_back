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
import org.example.app.web.model.MonthlyBalanceModel;
import org.example.app.web.model.ProportionalSettlementModel;
import org.example.app.web.model.UpcomingInstallmentModel;
import org.example.app.web.model.CreditCardBalanceModel;
import org.example.app.web.model.CreditCardProportionalPaymentModel;
import org.example.app.web.model.UserPaymentShare;
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
 * Servicio para la generación de reportes financieros y cálculos de saldos.
 * Implementa la "inteligencia" del sistema para análisis financiero.
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

    /**
     * Calcula el balance mensual (ingresos vs gastos) para un tenant.
     */
    public MonthlyBalanceModel getMonthlyBalance(Long tenantId, YearMonth yearMonth) {
        log.info("Calculando balance mensual para tenant: {} en {}", tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;

        for (Transaction transaction : transactions) {
            if ("INCOME".equals(transaction.getTransactionType())) {
                totalIncome = totalIncome.add(transaction.getAmount());
                incomeCount++;
            } else if ("EXPENSE".equals(transaction.getTransactionType())) {
                totalExpenses = totalExpenses.add(transaction.getAmount());
                expenseCount++;
            }
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        log.info("Balance calculado - Ingresos: ${}, Gastos: ${}, Balance neto: ${}",
                totalIncome, totalExpenses, netBalance);

        return new MonthlyBalanceModel(
            tenantId,
            yearMonth,
            totalIncome,
            totalExpenses,
            netBalance,
            incomeCount,
            expenseCount
        );
    }

    /**
     * Calcula la liquidación proporcional del mes.
     * LÓGICA CORE: Determina quién debe pagar a quién y cuánto.
     */
    public List<ProportionalSettlementModel> getProportionalSettlement(Long tenantId, YearMonth yearMonth) {
        log.info("Calculando liquidación proporcional para tenant: {} en {}", tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Obtener usuarios del tenant
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);

        // Obtener gastos compartidos del mes
        List<Transaction> sharedExpenses = transactionFacade.findSharedByTenantAndDateRange(tenantId, startDate, endDate);

        // Calcular cuánto gastó cada usuario
        Map<Long, BigDecimal> actualExpensesByUser = new HashMap<>();
        for (User user : users) {
            actualExpensesByUser.put(user.getId(), BigDecimal.ZERO);
        }

        BigDecimal totalSharedExpenses = BigDecimal.ZERO;
        for (Transaction expense : sharedExpenses) {
            Long userId = expense.getUser().getId();
            BigDecimal currentAmount = actualExpensesByUser.get(userId);
            actualExpensesByUser.put(userId, currentAmount.add(expense.getAmount()));
            totalSharedExpenses = totalSharedExpenses.add(expense.getAmount());
        }

        // Calcular cuánto debería haber pagado cada usuario según su porcentaje
        Map<Long, BigDecimal> expectedExpensesByUser = new HashMap<>();
        for (User user : users) {
            BigDecimal expectedAmount = totalSharedExpenses
                    .multiply(user.getContributionPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            expectedExpensesByUser.put(user.getId(), expectedAmount);
        }

        // Calcular diferencias y generar liquidaciones
        List<ProportionalSettlementModel> settlements = new ArrayList<>();

        for (User user : users) {
            BigDecimal actualExpense = actualExpensesByUser.get(user.getId());
            BigDecimal expectedExpense = expectedExpensesByUser.get(user.getId());
            BigDecimal difference = actualExpense.subtract(expectedExpense);

            settlements.add(new ProportionalSettlementModel(
                user.getId(),
                user.getName(),
                actualExpense,
                expectedExpense,
                difference,
                difference.compareTo(BigDecimal.ZERO) > 0 ? "DEBE_RECIBIR" : "DEBE_PAGAR"
            ));

            log.info("Usuario {}: Gastó ${}, Debería haber gastado ${}, Diferencia: ${}",
                    user.getName(), actualExpense, expectedExpense, difference);
        }

        return settlements;
    }

    /**
     * Obtiene las próximas cuotas MSI por vencer.
     */
    public List<UpcomingInstallmentModel> getUpcomingInstallments(Long tenantId, int nextMonths) {
        log.info("Obteniendo próximas cuotas MSI para tenant: {} en los próximos {} meses", tenantId, nextMonths);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(nextMonths);

        List<Installment> upcomingInstallments = installmentFacade
                .findPendingByTenantAndDateRange(tenantId, startDate, endDate);

        return upcomingInstallments.stream()
                .map(installment -> new UpcomingInstallmentModel(
                    installment.getId(),
                    installment.getTransaction().getId(),
                    installment.getTransaction().getDescription(),
                    installment.getInstallmentNumber(),
                    installment.getTransaction().getTotalInstallments(),
                    installment.getInstallmentAmount(),
                    installment.getProjectedDate(),
                    installment.getTransaction().getPaymentMethod().getBankName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene cuotas MSI vencidas.
     */
    public List<UpcomingInstallmentModel> getOverdueInstallments(Long tenantId) {
        log.info("Obteniendo cuotas MSI vencidas para tenant: {}", tenantId);

        LocalDate currentDate = LocalDate.now();

        List<Installment> overdueInstallments = installmentFacade
                .findOverdueByTenant(tenantId, currentDate);

        return overdueInstallments.stream()
                .map(installment -> new UpcomingInstallmentModel(
                    installment.getId(),
                    installment.getTransaction().getId(),
                    installment.getTransaction().getDescription(),
                    installment.getInstallmentNumber(),
                    installment.getTransaction().getTotalInstallments(),
                    installment.getInstallmentAmount(),
                    installment.getProjectedDate(),
                    installment.getTransaction().getPaymentMethod().getBankName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Resumen financiero completo del mes.
     */
    public Map<String, Object> getMonthlyFinancialSummary(Long tenantId, YearMonth yearMonth) {
        log.info("Generando resumen financiero completo para tenant: {} en {}", tenantId, yearMonth);

        Map<String, Object> summary = new HashMap<>();

        // Balance mensual
        MonthlyBalanceModel balance = getMonthlyBalance(tenantId, yearMonth);
        summary.put("monthlyBalance", balance);

        // Liquidación proporcional
        List<ProportionalSettlementModel> settlement = getProportionalSettlement(tenantId, yearMonth);
        summary.put("proportionalSettlement", settlement);

        // Próximas cuotas MSI (próximos 3 meses)
        List<UpcomingInstallmentModel> upcomingInstallments = getUpcomingInstallments(tenantId, 3);
        summary.put("upcomingInstallments", upcomingInstallments);

        // Cuotas vencidas
        List<UpcomingInstallmentModel> overdueInstallments = getOverdueInstallments(tenantId);
        summary.put("overdueInstallments", overdueInstallments);

        log.info("Resumen financiero generado exitosamente para tenant: {}", tenantId);
        return summary;
    }

    /**
     * Calcula el saldo actual de cada tarjeta de crédito de un tenant.
     * Considera la fecha de corte y la fecha de pago para determinar cuánto se debe.
     */
    public List<CreditCardBalanceModel> getCreditCardBalances(Long tenantId) {
        log.info("Calculando saldos de tarjetas de crédito para tenant: {}", tenantId);

        LocalDate today = LocalDate.now();

        // Obtener todos los usuarios del tenant
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);
        List<CreditCardBalanceModel> balances = new ArrayList<>();

        for (User user : users) {
            // Obtener métodos de pago del usuario (tarjetas de crédito)
            List<PaymentMethod> paymentMethods = paymentMethodFacade.findByUserIdAndActive(user.getId());

            log.debug("Usuario {}: tiene {} métodos de pago", user.getName(), paymentMethods.size());

            for (PaymentMethod pm : paymentMethods) {
                log.debug("Payment Method ID {}: bankName={}, accountType={}, cutDay={}",
                    pm.getId(), pm.getBankName(), pm.getAccountType(), pm.getCutDay());

                // Solo procesar tarjetas de crédito que tengan fecha de corte configurada
                if (!"CREDIT".equals(pm.getAccountType()) || pm.getCutDay() == null) {
                    log.warn("⚠️ Saltando Payment Method ID {}: accountType={}, cutDay={} - No cumple requisitos para calcular saldo",
                        pm.getId(), pm.getAccountType(), pm.getCutDay());
                    continue;
                }

                log.info("✅ Procesando tarjeta {} (ID: {})", pm.getBankName(), pm.getId());

                // Calcular fechas del ciclo actual
                LocalDate lastCutDate = calculateLastCutDate(today, pm.getCutDay());
                LocalDate previousCutDate = lastCutDate.minusMonths(1);
                LocalDate nextCutDate = lastCutDate.plusMonths(1);

                // Determinar el periodo a mostrar
                // Primero verificamos si el periodo del último corte ya está pagado
                String lastPeriodId = previousCutDate.plusDays(1) + "_" + lastCutDate;
                boolean lastPeriodPaid = periodPaymentRepository.existsByPaymentMethodIdAndPeriodId(pm.getId(), lastPeriodId);

                LocalDate rangeStart;
                LocalDate rangeEnd;
                LocalDate displayCutDate;
                LocalDate currentPaymentDate;
                String periodId;
                boolean isPaid;

                if (lastPeriodPaid) {
                    // Si el último periodo ya está pagado, mostrar el SIGUIENTE periodo
                    rangeStart = lastCutDate.plusDays(1);
                    rangeEnd = nextCutDate;
                    displayCutDate = nextCutDate;
                    currentPaymentDate = calculatePaymentDate(nextCutDate, pm.getPaymentDay());
                    periodId = rangeStart + "_" + rangeEnd;
                    isPaid = periodPaymentRepository.existsByPaymentMethodIdAndPeriodId(pm.getId(), periodId);

                    log.info("Tarjeta {}: Último periodo YA PAGADO, mostrando SIGUIENTE periodo: {} al {}, Fecha de pago={}",
                        pm.getBankName(), rangeStart, rangeEnd, currentPaymentDate);
                } else {
                    // Si el último periodo NO está pagado, mostrar ese periodo
                    rangeStart = previousCutDate.plusDays(1);
                    rangeEnd = lastCutDate;
                    displayCutDate = lastCutDate;
                    currentPaymentDate = calculatePaymentDate(lastCutDate, pm.getPaymentDay());
                    periodId = lastPeriodId;
                    isPaid = false;

                    log.info("Tarjeta {}: Mostrando periodo PENDIENTE: {} al {}, Fecha de pago={}",
                        pm.getBankName(), rangeStart, rangeEnd, currentPaymentDate);
                }

                // Obtener transacciones del periodo
                List<Transaction> transactions = transactionFacade.findByPaymentMethodAndDateRange(
                    pm.getId(), rangeStart, rangeEnd);

                // Calcular saldo de transacciones normales
                BigDecimal currentBalance = transactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Obtener cuotas MSI que vencen en este periodo de pago
                List<Installment> installments = installmentFacade.findPendingByPaymentMethodAndDateRange(
                    pm.getId(), rangeStart, rangeEnd);

                BigDecimal pendingInstallments = installments.stream()
                    .map(Installment::getInstallmentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalDue = currentBalance.add(pendingInstallments);

                // Determinar el paymentStatus basado en el estado y si está pagado
                String paymentStatus;
                if (isPaid) {
                    paymentStatus = "PAID";
                } else if (today.isAfter(currentPaymentDate)) {
                    // Si pasó la fecha de pago y NO está pagado → OVERDUE
                    paymentStatus = "OVERDUE";
                } else {
                    // Está entre el corte y la fecha de pago → PENDING
                    paymentStatus = "PENDING";
                }

                // Determinar el estado del ciclo
                String status;
                if (today.isBefore(displayCutDate)) {
                    status = "PENDING_CUT";
                } else if (today.isBefore(currentPaymentDate) || today.isEqual(currentPaymentDate)) {
                    status = "PENDING_PAYMENT";
                } else {
                    status = "OVERDUE";
                }

                // Calcular días hasta el corte que se está mostrando (displayCutDate) y hasta la fecha de pago
                // Usar `displayCutDate` para que cuando el corte mostrado ya pasó el valor sea negativo.
                int daysUntilCut = (int) ChronoUnit.DAYS.between(today, displayCutDate);
                int daysUntilPayment = (int) ChronoUnit.DAYS.between(today, currentPaymentDate);

                log.info("Tarjeta {}: today={}, displayCutDate={}, nextCutDate={}, currentPaymentDate={}, daysUntilCut={}, daysUntilPayment={}, isPaid={}",
                    pm.getBankName(), today, displayCutDate, nextCutDate, currentPaymentDate, daysUntilCut, daysUntilPayment, isPaid);

                CreditCardBalanceModel balance = new CreditCardBalanceModel(
                    pm.getId(),
                    // propietario de la tarjeta
                    pm.getUser() != null ? pm.getUser().getId() : null,
                    // alias de la tarjeta
                    pm.getAlias(),
                    pm.getBankName(),
                    pm.getAccountType(),
                    pm.getCutDay(),
                    pm.getPaymentDay(),
                    displayCutDate,  // Usar displayCutDate en lugar de lastCutDate
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
                );

                balances.add(balance);

                log.info("Tarjeta {}: Saldo ${}, Cuotas MSI ${}, Total ${}, Status: {}, PaymentStatus: {}, Pagado: {}, PeriodId: {}",
                    pm.getBankName(), currentBalance, pendingInstallments, totalDue, status, paymentStatus, isPaid, periodId);
            }
        }

        return balances;
    }

    /**
     * Calcula cuánto debe pagar cada usuario por cada tarjeta de crédito según su porcentaje de contribución.
     * Similar a la liquidación proporcional pero aplicado a los saldos de tarjetas.
     */
    public List<CreditCardProportionalPaymentModel> getCreditCardProportionalPayments(Long tenantId) {
        log.info("Calculando pagos proporcionales de tarjetas para tenant: {}", tenantId);

         // Obtener todos los usuarios del tenant
         List<User> users = userFacade.findByTenantIdAndActive(tenantId);
         List<CreditCardProportionalPaymentModel> result = new ArrayList<>();

         // Obtener los saldos de todas las tarjetas
         List<CreditCardBalanceModel> cardBalances = getCreditCardBalances(tenantId);

         for (CreditCardBalanceModel card : cardBalances) {
            // Calcular cuánto debe pagar cada usuario según su porcentaje
            List<UserPaymentShare> userShares = new ArrayList<>();

            for (User user : users) {
                BigDecimal userPercentage = user.getContributionPercentage();
                BigDecimal userAmount = card.getTotalDue()
                    .multiply(userPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                userShares.add(new UserPaymentShare(
                    user.getId(),
                    user.getName(),
                    userPercentage,
                    userAmount
                ));

                log.info("Tarjeta {}: Usuario {} ({}%) debe pagar ${} de ${} total",
                    card.getBankName(), user.getName(), userPercentage, userAmount, card.getTotalDue());
            }

            CreditCardProportionalPaymentModel payment = new CreditCardProportionalPaymentModel(
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
            );

            result.add(payment);
        }

        log.info("Calculados pagos proporcionales para {} tarjetas", result.size());
        return result;
    }

    /**
     * Marca un periodo de tarjeta de crédito como pagado.
     */
    public void markPeriodAsPaid(Long paymentMethodId, String periodId) {
        log.info("Marcando periodo {} como pagado para tarjeta {}", periodId, paymentMethodId);

        // Verificar si ya existe
        if (periodPaymentRepository.existsByPaymentMethodIdAndPeriodId(paymentMethodId, periodId)) {
            log.warn("El periodo {} ya estaba marcado como pagado", periodId);
            return;
        }

        // Extraer fechas del periodId: "2026-01-03_2026-02-02"
        String[] dates = periodId.split("_");
        LocalDate periodStart = LocalDate.parse(dates[0]);
        LocalDate periodEnd = LocalDate.parse(dates[1]);

        // Buscar el payment method
        PaymentMethod pm = paymentMethodFacade.findById(paymentMethodId)
            .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado: " + paymentMethodId));

        // Crear registro de pago
        CreditCardPeriodPayment payment = new CreditCardPeriodPayment(pm, periodId, periodStart, periodEnd);
        periodPaymentRepository.save(payment);

        log.info("✅ Periodo {} marcado como pagado exitosamente", periodId);
    }

    /**
     * Calcula la fecha de corte actual o próxima basándose en el día de corte.
     */
    private LocalDate calculateCurrentCutDate(LocalDate today, Integer cutDay) {
        LocalDate cutDate = today.withDayOfMonth(Math.min(cutDay, today.lengthOfMonth()));

        // Si hoy es el día de corte, usar la fecha de hoy como último corte
        if (today.isEqual(cutDate)) {
            return today;
        }

        // Si ya pasó el día de corte este mes, usar el del próximo mes
        if (today.isAfter(cutDate)) {
            cutDate = cutDate.plusMonths(1);
            cutDate = cutDate.withDayOfMonth(Math.min(cutDay, cutDate.lengthOfMonth()));
        }

        return cutDate;
    }

    /**
     * Calcula la fecha de pago basándose en la fecha de corte y el día de pago.
     * Ejemplos:
     * - Corte día 2, Pago día 21 → mismo mes (2 feb → 21 feb)
     * - Corte día 25, Pago día 15 → mes siguiente (25 feb → 15 mar)
     */
    private LocalDate calculatePaymentDate(LocalDate cutDate, Integer paymentDay) {
        if (paymentDay == null) {
            // Si no hay día de pago configurado, asumir 20 días después del corte
            return cutDate.plusDays(20);
        }

        int cutDay = cutDate.getDayOfMonth();
        LocalDate paymentDate;

        if (paymentDay > cutDay) {
            // El pago es en el mismo mes que el corte
            // Ejemplo: corte día 2, pago día 21 → 21 de febrero
            paymentDate = cutDate.withDayOfMonth(Math.min(paymentDay, cutDate.lengthOfMonth()));
        } else {
            // El pago es en el mes siguiente al corte
            // Ejemplo: corte día 25, pago día 15 → 15 de marzo
            paymentDate = cutDate.plusMonths(1);
            paymentDate = paymentDate.withDayOfMonth(Math.min(paymentDay, paymentDate.lengthOfMonth()));
        }

        return paymentDate;
    }

    /**
     * Calcula la fecha del último corte que ya pasó basándose en el día de corte.
     */
    private LocalDate calculateLastCutDate(LocalDate today, Integer cutDay) {
        LocalDate cutDate = today.withDayOfMonth(Math.min(cutDay, today.lengthOfMonth()));

        // Si hoy es antes del día de corte de este mes, el último corte fue el mes pasado
        if (today.isBefore(cutDate)) {
            cutDate = cutDate.minusMonths(1);
            cutDate = cutDate.withDayOfMonth(Math.min(cutDay, cutDate.lengthOfMonth()));
        }
        // Si hoy es igual o después del día de corte, el último corte fue este mes

        return cutDate;
    }
}
