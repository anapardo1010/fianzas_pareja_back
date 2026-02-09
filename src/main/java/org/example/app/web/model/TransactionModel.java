package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.app.domain.entity.Transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Modelo de respuesta para Transaction.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class TransactionModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long tenantId;
    private final Long userId;
    private final Long categoryId;
    private final Long paymentMethodId;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate date;
    private final Boolean isShared;
    private final String transactionType;
    private final Boolean hasInstallments;
    private final Integer totalInstallments;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Función para convertir Entity a Model.
     */
    public static final Function<Transaction, TransactionModel> FN_ENTITY_TO_MODEL = entity ->
        new TransactionModel(
            entity.getId(),
            entity.getTenant().getId(),
            entity.getUser().getId(),
            entity.getCategory() != null ? entity.getCategory().getId() : null,
            entity.getPaymentMethod() != null ? entity.getPaymentMethod().getId() : null,
            entity.getDescription(),
            entity.getAmount(),
            entity.getDate(),
            entity.getIsShared(),
            entity.getTransactionType(),
            entity.getHasInstallments(),
            entity.getTotalInstallments(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );

    @Override
    public String toString() {
        return "TransactionModel{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", userId=" + userId +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", isShared=" + isShared +
                ", transactionType='" + transactionType + '\'' +
                ", hasInstallments=" + hasInstallments +
                '}';
    }
}
