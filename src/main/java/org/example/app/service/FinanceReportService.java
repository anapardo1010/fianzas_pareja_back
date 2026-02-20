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
import org.example.app.web.model.PaymentMethodBalanceModel;
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
     * Compatibilidad: firma original sin 'mode'. Usa 'accrual' por defecto.
     */
    public MonthlyBalanceModel getMonthlyBalance(Long tenantId, YearMonth yearMonth) {
        return getMonthlyBalance(tenantId, yearMonth, "accrual");
    }

    /**
     * Calcula el balance mensual (ingresos vs gastos) para un tenant.
     */
    public MonthlyBalanceModel getMonthlyBalance(Long tenantId, YearMonth yearMonth, String mode) {
        log.info("Calculando balance mensual (modo={}) para tenant: {} en {}", mode, tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;

        boolean accrual = mode == null || "accrual".equalsIgnoreCase(mode);

        for (Transaction transaction : transactions) {
            String type = transaction.getTransactionType();

            // Excluir TRANSFER del balance general (no son ingresos ni gastos reales)
            if ("TRANSFER".equalsIgnoreCase(type)) {
                continue;
            }

            if ("INCOME".equalsIgnoreCase(type)) {
                totalIncome = totalIncome.add(transaction.getAmount());
                incomeCount++;
                continue;
            }

            if ("EXPENSE".equalsIgnoreCase(type) || "CREDIT_PAYMENT".equalsIgnoreCase(type)) {
                // Determinar si es compra con tarjeta de crédito
                String accountType = transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().getAccountType() : null;
                boolean isCreditCardPurchase = "CREDIT".equalsIgnoreCase(accountType) && "EXPENSE".equalsIgnoreCase(type);
                boolean isCreditCardPayment = "CREDIT_PAYMENT".equalsIgnoreCase(type);

                if (accrual) {
                    // Base devengo: contar compras (incluyendo en tarjeta), excluir pagos de tarjeta
                    if (isCreditCardPayment) {
                        // excluir del gasto para evitar doble conteo
                        continue;
                    }
                    totalExpenses = totalExpenses.add(transaction.getAmount());
                    expenseCount++;
                } else {
                    // Base caja: contar salida de efectivo
                    // Excluir compras en tarjeta de crédito (se contabilizan al pagar)
                    if (isCreditCardPurchase) {
                        continue;
                    }
                    // Incluir pagos de tarjeta y gastos pagados en débito/efectivo
                    totalExpenses = totalExpenses.add(transaction.getAmount());
                    expenseCount++;
                }
            }
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        log.info("Balance calculado ({}): Ingresos: ${}, Gastos: ${}, Balance neto: ${}",
                accrual ? "accrual" : "cash", totalIncome, totalExpenses, netBalance);

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

        // Balance mensual (por defecto modo accrual)
        MonthlyBalanceModel balance = getMonthlyBalance(tenantId, yearMonth, "accrual");
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
     * Calcula cuánto debe pagar cada usuario por cada tarjeta de crédito.
     * Las transacciones compartidas se dividen proporcionalmente según porcentajes de contribución.
     * Las transacciones no compartidas se asignan 100% al usuario que las realizó.
     * Lo mismo aplica para las cuotas MSI, basado en si la transacción original es compartida.
     */
    public List<CreditCardProportionalPaymentModel> getCreditCardProportionalPayments(Long tenantId) {
        log.info("Calculando pagos proporcionales de tarjetas para tenant: {}", tenantId);

         // Obtener todos los usuarios del tenant
         List<User> users = userFacade.findByTenantIdAndActive(tenantId);
         List<CreditCardProportionalPaymentModel> result = new ArrayList<>();

         // Obtener los saldos de todas las tarjetas
         List<CreditCardBalanceModel> cardBalances = getCreditCardBalances(tenantId);

         for (CreditCardBalanceModel card : cardBalances) {
            // Parsear el periodo para obtener rangeStart y rangeEnd
            String[] periodParts = card.getPeriodId().split("_");
            LocalDate rangeStart = LocalDate.parse(periodParts[0]);
            LocalDate rangeEnd = LocalDate.parse(periodParts[1]);

            // Obtener transacciones del periodo para esta tarjeta
            List<Transaction> transactions = transactionFacade.findByPaymentMethodAndDateRange(
                card.getPaymentMethodId(), rangeStart, rangeEnd);

            // Obtener cuotas MSI del periodo para esta tarjeta
            List<Installment> installments = installmentFacade.findPendingByPaymentMethodAndDateRange(
                card.getPaymentMethodId(), rangeStart, rangeEnd);

            // Agrupar sumas de transacciones por usuario
            Map<Long, BigDecimal> userTransactionSums = new HashMap<>();
            for (User user : users) {
                userTransactionSums.put(user.getId(), BigDecimal.ZERO);
            }
            for (Transaction transaction : transactions) {
                if (transaction.getIsShared()) {
                    // Dividir proporcionalmente
                    for (User user : users) {
                        BigDecimal userShare = transaction.getAmount()
                            .multiply(user.getContributionPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        userTransactionSums.put(user.getId(), userTransactionSums.get(user.getId()).add(userShare));
                    }
                } else {
                    // Asignar 100% al usuario que la realizó
                    Long userId = transaction.getUser().getId();
                    userTransactionSums.put(userId, userTransactionSums.get(userId).add(transaction.getAmount()));
                }
            }

            // Agrupar sumas de cuotas por usuario
            Map<Long, BigDecimal> userInstallmentSums = new HashMap<>();
            for (User user : users) {
                userInstallmentSums.put(user.getId(), BigDecimal.ZERO);
            }
            for (Installment installment : installments) {
                if (installment.getTransaction().getIsShared()) {
                    // Dividir proporcionalmente
                    for (User user : users) {
                        BigDecimal userShare = installment.getInstallmentAmount()
                            .multiply(user.getContributionPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        userInstallmentSums.put(user.getId(), userInstallmentSums.get(user.getId()).add(userShare));
                    }
                } else {
                    // Asignar 100% al usuario de la transacción original
                    Long userId = installment.getTransaction().getUser().getId();
                    userInstallmentSums.put(userId, userInstallmentSums.get(userId).add(installment.getInstallmentAmount()));
                }
            }

            // Calcular cuánto debe pagar cada usuario
            List<UserPaymentShare> userShares = new ArrayList<>();

            for (User user : users) {
                BigDecimal userTransactionAmount = userTransactionSums.getOrDefault(user.getId(), BigDecimal.ZERO);
                BigDecimal userInstallmentAmount = userInstallmentSums.getOrDefault(user.getId(), BigDecimal.ZERO);
                BigDecimal userAmount = userTransactionAmount.add(userInstallmentAmount);
                BigDecimal userPercentage = BigDecimal.valueOf(100); // Paga 100% de su parte asignada

                userShares.add(new UserPaymentShare(
                    user.getId(),
                    user.getName(),
                    userPercentage,
                    userAmount
                ));

                log.info("Tarjeta {}: Usuario {} debe pagar ${} (transacciones ${} + cuotas ${})",
                    card.getBankName(), user.getName(), userAmount, userTransactionAmount, userInstallmentAmount);
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

    /**
     * Calcula el balance individual de cada método de pago para un tenant.
     * Útil para saber cuánto efectivo, cuánto en débito, cuánto debo en cada tarjeta.
     */
    public List<PaymentMethodBalanceModel> getBalanceByPaymentMethod(Long tenantId, YearMonth yearMonth) {
        log.info("Calculando balance por método de pago para tenant: {} en {}", tenantId, yearMonth);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionFacade.findByTenantAndDateRange(tenantId, startDate, endDate);

        // Obtener todos los métodos de pago del tenant
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);
        Map<Long, PaymentMethodBalanceModel> balanceMap = new HashMap<>();

        // Inicializar con todos los métodos de pago
        for (User user : users) {
            List<PaymentMethod> paymentMethods = paymentMethodFacade.findByUserIdAndActive(user.getId());
            for (PaymentMethod pm : paymentMethods) {
                balanceMap.put(pm.getId(), new PaymentMethodBalanceModel(
                    pm.getId(),
                    pm.getBankName(),
                    pm.getAlias(),
                    pm.getAccountType(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0
                ));
            }
        }

        // Procesar transacciones
        for (Transaction transaction : transactions) {
            String type = transaction.getTransactionType();
            BigDecimal amount = transaction.getAmount();
            Long pmId = transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().getId() : null;
            Long destPmId = transaction.getDestinationPaymentMethod() != null ? transaction.getDestinationPaymentMethod().getId() : null;

            if (pmId != null && !balanceMap.containsKey(pmId)) {
                // Si el método de pago no está en el mapa, crear entrada
                PaymentMethod pm = transaction.getPaymentMethod();
                balanceMap.put(pmId, new PaymentMethodBalanceModel(
                    pm.getId(),
                    pm.getBankName(),
                    pm.getAlias(),
                    pm.getAccountType(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0
                ));
            }

            if ("INCOME".equalsIgnoreCase(type) && pmId != null) {
                // Ingreso: suma al método de pago
                PaymentMethodBalanceModel model = balanceMap.get(pmId);
                model.setTotalIncome(model.getTotalIncome().add(amount));
                model.setBalance(model.getBalance().add(amount));
                model.setTransactionCount(model.getTransactionCount() + 1);
            } else if ("EXPENSE".equalsIgnoreCase(type) && pmId != null) {
                // Gasto: resta al método de pago
                PaymentMethodBalanceModel model = balanceMap.get(pmId);
                model.setTotalExpenses(model.getTotalExpenses().add(amount));
                model.setBalance(model.getBalance().subtract(amount));
                model.setTransactionCount(model.getTransactionCount() + 1);
            } else if ("CREDIT_PAYMENT".equalsIgnoreCase(type) && pmId != null) {
                // Pago de tarjeta: resta al método de pago origen (débito/efectivo)
                PaymentMethodBalanceModel model = balanceMap.get(pmId);
                model.setTotalExpenses(model.getTotalExpenses().add(amount));
                model.setBalance(model.getBalance().subtract(amount));
                model.setTransactionCount(model.getTransactionCount() + 1);
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                // Transferencia: resta del origen, suma al destino
                if (pmId != null) {
                    PaymentMethodBalanceModel sourceModel = balanceMap.get(pmId);
                    sourceModel.setTransfersOut(sourceModel.getTransfersOut().add(amount));
                    sourceModel.setBalance(sourceModel.getBalance().subtract(amount));
                    sourceModel.setTransactionCount(sourceModel.getTransactionCount() + 1);
                }
                if (destPmId != null) {
                    if (!balanceMap.containsKey(destPmId)) {
                        PaymentMethod destPm = transaction.getDestinationPaymentMethod();
                        balanceMap.put(destPmId, new PaymentMethodBalanceModel(
                            destPm.getId(),
                            destPm.getBankName(),
                            destPm.getAlias(),
                            destPm.getAccountType(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0
                        ));
                    }
                    PaymentMethodBalanceModel destModel = balanceMap.get(destPmId);
                    destModel.setTransfersIn(destModel.getTransfersIn().add(amount));
                    destModel.setBalance(destModel.getBalance().add(amount));
                }
            }
        }

        List<PaymentMethodBalanceModel> result = new ArrayList<>(balanceMap.values());
        log.info("Balance calculado para {} métodos de pago", result.size());
        return result;
    }

    /**
     * Calcula pagos proporcionales para métodos de pago que NO son tarjetas de crédito
     * (efectivo, débito, cuentas) en un rango de fechas.
     * Si startDate o endDate son null, usa el mes actual.
     */
    public List<org.example.app.web.model.PaymentMethodProportionalPaymentModel> getNonCreditPaymentMethodProportionalPayments(
            Long tenantId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculando pagos proporcionales (no-credit) para tenant: {} en rango: {} - {}", tenantId, startDate, endDate);

        // Determinar rango por defecto (mes actual) si no se proporcionó
        LocalDate now = LocalDate.now();
        if (startDate == null || endDate == null) {
            YearMonth ym = YearMonth.of(now.getYear(), now.getMonthValue());
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }

        // Obtener usuarios activos
        List<User> users = userFacade.findByTenantIdAndActive(tenantId);

        List<org.example.app.web.model.PaymentMethodProportionalPaymentModel> result = new ArrayList<>();

        // Recolectar todos los métodos de pago por usuario y filtrar los que NO son CREDIT
        for (User user : users) {
            List<PaymentMethod> paymentMethods = paymentMethodFacade.findByUserIdAndActive(user.getId());

            for (PaymentMethod pm : paymentMethods) {
                if ("CREDIT".equalsIgnoreCase(pm.getAccountType())) {
                    // Saltar tarjetas de crédito
                    continue;
                }

                // Obtener transacciones para este método en el rango
                List<Transaction> transactions = transactionFacade.findByPaymentMethodAndDateRange(pm.getId(), startDate, endDate);

                // Inicializar sumas por usuario
                Map<Long, BigDecimal> userTransactionSums = new HashMap<>();
                for (User u : users) {
                    userTransactionSums.put(u.getId(), BigDecimal.ZERO);
                }

                BigDecimal totalExpenses = BigDecimal.ZERO;
                int txCount = 0;

                for (Transaction tx : transactions) {
                    // Sólo nos interesan EXPENSE que sean gastos reales y que NO sean parte de cuotas/installments
                    if (!"EXPENSE".equalsIgnoreCase(tx.getTransactionType())) {
                        // Ignorar INCOME, TRANSFER, CREDIT_PAYMENT, etc.
                        continue;
                    }

                    // Ignorar transacciones que tienen installments (p. ej. MSI) — se tratan en el módulo de tarjetas
                    if (tx.getHasInstallments() != null && tx.getHasInstallments()) {
                        continue;
                    }

                    BigDecimal amt = tx.getAmount();

                    // Contabilizar como gasto real
                    totalExpenses = totalExpenses.add(amt);
                    txCount++;

                    if (tx.getIsShared()) {
                        // Dividir proporcionalmente entre todos los usuarios según su porcentaje
                        for (User u : users) {
                            BigDecimal share = amt.multiply(u.getContributionPercentage())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            userTransactionSums.put(u.getId(), userTransactionSums.get(u.getId()).add(share));
                        }
                    } else {
                        // Asignar 100% al usuario que realizó la transacción
                        Long txUserId = tx.getUser() != null ? tx.getUser().getId() : null;
                        if (txUserId != null) {
                            userTransactionSums.put(txUserId, userTransactionSums.get(txUserId).add(amt));
                        }
                    }
                }

                // Construir userShares
                List<org.example.app.web.model.UserPaymentShare> userShares = new ArrayList<>();
                for (User u : users) {
                    BigDecimal amountToPay = userTransactionSums.getOrDefault(u.getId(), BigDecimal.ZERO);
                    BigDecimal payPercent = u.getContributionPercentage() != null ? u.getContributionPercentage() : BigDecimal.valueOf(100);
                    userShares.add(new org.example.app.web.model.UserPaymentShare(
                            u.getId(), u.getName(), payPercent, amountToPay
                    ));

                    log.info("No-credit PM {}: Usuario {} debe pagar ${}", pm.getAlias(), u.getName(), amountToPay);
                }

                org.example.app.web.model.PaymentMethodProportionalPaymentModel model = new org.example.app.web.model.PaymentMethodProportionalPaymentModel(
                        pm.getId(),
                        pm.getUser() != null ? pm.getUser().getId() : null,
                        pm.getAlias(),
                        pm.getBankName(),
                        pm.getAccountType(),
                        startDate,
                        endDate,
                        totalExpenses, // ahora representa el total gastado en el periodo (no balance bancario)
                        txCount,
                        userShares
                );

                result.add(model);
            }
        }

        log.info("Calculados pagos proporcionales (no-credit) para {} métodos", result.size());
        return result;
    }
}
