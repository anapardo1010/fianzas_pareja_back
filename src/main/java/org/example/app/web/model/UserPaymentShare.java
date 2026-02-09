package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modelo que representa cuánto debe pagar un usuario específico de una tarjeta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPaymentShare {

    private Long userId;
    private String userName;
    private BigDecimal contributionPercentage;
    private BigDecimal amountToPay;
}

