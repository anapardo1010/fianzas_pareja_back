package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Modelo para crear un nuevo PaymentMethod dentro de un usuario.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class PaymentMethodCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String bankName;
    private final String alias; // alias opcional para identificar la tarjeta (ej. "Visa personal")
    private final String accountType; // CREDIT, DEBIT, CASH
    private final Integer cutDay;
    private final Integer paymentDay;

    @Override
    public String toString() {
        return "PaymentMethodCreateModel{" +
                "userId=" + userId +
                ", bankName='" + (bankName != null ? bankName.replaceAll(".", "*") : null) + '\'' +
                ", alias='" + alias + '\'' +
                ", accountType='" + accountType + '\'' +
                ", cutDay=" + cutDay +
                ", paymentDay=" + paymentDay +
                '}';
    }
}
