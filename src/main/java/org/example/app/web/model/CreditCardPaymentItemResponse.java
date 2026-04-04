package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un ítem de pago de tarjeta de crédito en la respuesta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardPaymentItemResponse {

    private Long id;
    private Long creditCardId;
    private String creditCardAlias;
    private String creditCardBankName;
    private String billingPeriodId;
    private Long sourcePaymentMethodId;
    private String sourcePaymentMethodAlias;
    private String sourcePaymentMethodBankName;
    private Long transactionId;
    private Long paidByUserId;
    private String paidByUserName;
    private BigDecimal amount;
    private String notes;
    private LocalDateTime paidAt;
}

