package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo que representa el saldo actual de una tarjeta de crédito.
 * Incluye información del periodo de corte y fecha de pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardBalanceModel {

    private Long paymentMethodId;
    private Long userId; // ID del usuario propietario de la tarjeta
    private String alias; // alias de la tarjeta para identificarla (p.ej. 'Visa personal')
    private String bankName;
    private String accountType;
    private Integer cutDay;
    private Integer paymentDay;

    // Fechas del ciclo actual
    private LocalDate currentCutDate;
    private LocalDate currentPaymentDate;
    private LocalDate nextCutDate;

    // Saldos
    private BigDecimal currentBalance;           // Saldo desde el último corte hasta hoy
    private BigDecimal pendingInstallments;      // Cuotas MSI del periodo
    private BigDecimal totalDue;                 // Total a pagar (balance + cuotas)

    // Contadores
    private Integer transactionCount;
    private Integer installmentCount;

    // Estado
    private String status; // PENDING_CUT, PENDING_PAYMENT, OVERDUE
    private Integer daysUntilCut;
    private Integer daysUntilPayment;

    // Nuevos campos para control de pagos
    private String paymentStatus;  // PAID, PENDING, OVERDUE
    private Boolean isPaid;        // true si el periodo ya fue marcado como pagado
    private String periodId;       // identificador único: "2026-01-03_2026-02-02"
}
