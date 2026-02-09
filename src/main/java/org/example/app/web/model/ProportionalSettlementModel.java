package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Modelo de respuesta para la liquidación proporcional.
 * Representa cuánto debe pagar o recibir cada usuario del tenant.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ProportionalSettlementModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String userName;
    private final BigDecimal actualExpense;
    private final BigDecimal expectedExpense;
    private final BigDecimal difference;
    private final String settlementType; // DEBE_RECIBIR, DEBE_PAGAR

    @Override
    public String toString() {
        return "ProportionalSettlementModel{" +
                "userId=" + userId +
                ", userName='" + (userName != null ? userName.replaceAll(".", "*") : null) + '\'' +
                ", actualExpense=" + actualExpense +
                ", expectedExpense=" + expectedExpense +
                ", difference=" + difference +
                ", settlementType='" + settlementType + '\'' +
                '}';
    }
}
