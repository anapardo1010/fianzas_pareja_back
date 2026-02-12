package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modelo que representa el balance de un método de pago específico.
 * Incluye ingresos, gastos, transferencias entrantes y salientes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodBalanceModel {

    private Long paymentMethodId;
    private String paymentMethodName;
    private String alias;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal transfersIn;
    private BigDecimal transfersOut;
    private int transactionCount;
}

