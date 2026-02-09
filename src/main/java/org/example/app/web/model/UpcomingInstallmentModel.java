package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de respuesta para cuotas MSI próximas.
 * Representa las próximas cuotas de meses sin intereses por vencer.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class UpcomingInstallmentModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long installmentId;
    private final Long transactionId;
    private final String transactionDescription;
    private final Integer installmentNumber;
    private final Integer totalInstallments;
    private final BigDecimal installmentAmount;
    private final LocalDate projectedDate;
    private final String paymentMethodName;

    @Override
    public String toString() {
        return "UpcomingInstallmentModel{" +
                "installmentId=" + installmentId +
                ", transactionId=" + transactionId +
                ", transactionDescription='" + (transactionDescription != null ? transactionDescription.replaceAll(".", "*") : null) + '\'' +
                ", installmentNumber=" + installmentNumber +
                ", totalInstallments=" + totalInstallments +
                ", installmentAmount=" + installmentAmount +
                ", projectedDate=" + projectedDate +
                ", paymentMethodName='" + (paymentMethodName != null ? paymentMethodName.replaceAll(".", "*") : null) + '\'' +
                '}';
    }
}
