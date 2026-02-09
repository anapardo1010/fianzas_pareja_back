package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo para crear una nueva Transaction.
 * Incluye lógica para gastos compartidos y MSI.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class TransactionCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long tenantId;
    private final Long userId;
    private final Long categoryId;
    private final Long paymentMethodId;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate date;
    private final Boolean isShared;
    private final String transactionType; // INCOME, EXPENSE
    private final Boolean hasInstallments;
    private final Integer totalInstallments;

    @Override
    public String toString() {
        return "TransactionCreateModel{" +
                "tenantId=" + tenantId +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", paymentMethodId=" + paymentMethodId +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", isShared=" + isShared +
                ", transactionType='" + transactionType + '\'' +
                ", hasInstallments=" + hasInstallments +
                ", totalInstallments=" + totalInstallments +
                '}';
    }
}
