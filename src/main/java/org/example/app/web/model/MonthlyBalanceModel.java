package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Modelo de respuesta para el balance mensual.
 * Representa ingresos vs gastos de un tenant en un mes específico.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class MonthlyBalanceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long tenantId;
    private final YearMonth yearMonth;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpenses;
    private final BigDecimal netBalance;
    private final Integer incomeTransactionCount;
    private final Integer expenseTransactionCount;

    @Override
    public String toString() {
        return "MonthlyBalanceModel{" +
                "tenantId=" + tenantId +
                ", yearMonth=" + yearMonth +
                ", totalIncome=" + totalIncome +
                ", totalExpenses=" + totalExpenses +
                ", netBalance=" + netBalance +
                ", incomeTransactionCount=" + incomeTransactionCount +
                ", expenseTransactionCount=" + expenseTransactionCount +
                '}';
    }
}
