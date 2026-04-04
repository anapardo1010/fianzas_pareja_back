package org.example.app.web.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Un ítem dentro del request de pago de tarjeta:
 * indica con qué método de pago se paga y cuánto.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreditCardPaymentItemRequest {

    /** ID del método de pago de origen (débito, efectivo, etc.) */
    private Long sourcePaymentMethodId;

    /** ID del usuario que realiza este pago */
    private Long paidByUserId;

    /** Monto que se paga con este método */
    private BigDecimal amount;

    /** Nota opcional */
    private String notes;
}

