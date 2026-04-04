package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Detalle completo de todos los cargos que forman el total a pagar
 * de una tarjeta de crédito en su periodo activo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardPeriodDetailModel {

    /** ID del método de pago (tarjeta) */
    private Long paymentMethodId;

    /** Alias de la tarjeta */
    private String alias;

    /** Banco */
    private String bankName;

    /** Inicio del periodo activo */
    private LocalDate periodStart;

    /** Fin del periodo activo (fecha de corte) */
    private LocalDate periodEnd;

    /** Fecha límite de pago */
    private LocalDate paymentDate;

    /** ID del periodo (ej: 2026-03-02_2026-04-01) */
    private String periodId;

    /** Estado del periodo */
    private String status;

    /** Estado de pago */
    private String paymentStatus;

    /** Suma de transacciones directas (sin MSI) */
    private BigDecimal directChargesTotal;

    /** Suma de cuotas MSI que caen en este periodo */
    private BigDecimal installmentsTotal;

    /** Total a pagar (directChargesTotal + installmentsTotal) */
    private BigDecimal totalDue;

    /** Lista de transacciones directas del periodo */
    private List<ChargeItem> directCharges;

    /** Lista de cuotas MSI que caen en este periodo */
    private List<InstallmentItem> installmentCharges;

    // -------------------------------------------------------------------------

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeItem {
        private Long transactionId;
        private String description;
        private BigDecimal amount;
        private LocalDate date;
        private Boolean isShared;
        private String categoryName;
        private Long userId;
        private String userName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentItem {
        private Long installmentId;
        private Long transactionId;
        private String description;
        private Integer installmentNumber;
        private Integer totalInstallments;
        private BigDecimal installmentAmount;
        private BigDecimal originalAmount;
        private LocalDate projectedDate;
        private Boolean isShared;
        private Long userId;
        private String userName;
    }
}

