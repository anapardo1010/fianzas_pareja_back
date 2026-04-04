package org.example.app.web.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para registrar el pago de una tarjeta de crédito en un periodo.
 * La suma de payments.amount debe ser igual a totalDue.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreditCardPaymentRequest {

    /** ID de la tarjeta de crédito que se va a pagar */
    private Long creditCardId;

    /** Periodo de facturación, ej: "2026-03-02_2026-04-01" */
    private String periodId;

    /** Total que debe cubrirse (para validación en el back) */
    private BigDecimal totalDue;

    /** Lista de pagos parciales por método de origen */
    private List<CreditCardPaymentItemRequest> payments;
}

