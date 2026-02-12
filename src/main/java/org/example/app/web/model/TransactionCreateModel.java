package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo para crear una nueva Transaction.
 * Incluye lógica para gastos compartidos, MSI y transferencias.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreateModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long userId;
    private Long categoryId;
    private Long paymentMethodId;
    private Long destinationPaymentMethodId; // Para transferencias entre métodos de pago
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private Boolean isShared;
    private String transactionType; // INCOME, EXPENSE, CREDIT_PAYMENT, TRANSFER
    private Boolean hasInstallments;
    private Integer totalInstallments;

    @Override
    public String toString() {
        return "TransactionCreateModel{" +
                "tenantId=" + tenantId +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", paymentMethodId=" + paymentMethodId +
                ", destinationPaymentMethodId=" + destinationPaymentMethodId +
                ", description='" + (description != null ? "***" : null) + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", isShared=" + isShared +
                ", transactionType='" + transactionType + '\'' +
                ", hasInstallments=" + hasInstallments +
                ", totalInstallments=" + totalInstallments +
                '}';
    }
}
