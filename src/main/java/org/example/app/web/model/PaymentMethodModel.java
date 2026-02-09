package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.app.domain.entity.PaymentMethod;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Modelo de respuesta para PaymentMethod.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class PaymentMethodModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long userId;
    private final String alias;
    private final String bankName;
    private final String accountType;
    private final Integer cutDay;
    private final Integer paymentDay;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Función para convertir Entity a Model.
     */
    public static final Function<PaymentMethod, PaymentMethodModel> FN_ENTITY_TO_MODEL = entity ->
        new PaymentMethodModel(
            entity.getId(),
            entity.getUser().getId(),
            entity.getAlias(),
            entity.getBankName(),
            entity.getAccountType(),
            entity.getCutDay(),
            entity.getPaymentDay(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );

    /**
     * Función para convertir Model a Entity (requiere user).
     */
    public static Function<PaymentMethodModel, PaymentMethod> fnModelToEntity(org.example.app.domain.entity.User user) {
        return model -> {
            PaymentMethod entity = new PaymentMethod(user, model.getBankName(), model.getAccountType(),
                                                   model.getCutDay(), model.getPaymentDay(), model.getIsActive());
            entity.setAlias(model.getAlias());
            entity.setId(model.getId());
            entity.setCreatedAt(model.getCreatedAt());
            entity.setUpdatedAt(model.getUpdatedAt());
            return entity;
        };
    }

    @Override
    public String toString() {
        return "PaymentMethodModel{" +
                "id=" + id +
                ", userId=" + userId +
                ", alias='" + alias + '\'' +
                ", bankName='" + (bankName != null ? bankName.replaceAll(".", "*") : null) + '\'' +
                ", accountType='" + accountType + '\'' +
                ", cutDay=" + cutDay +
                ", paymentDay=" + paymentDay +
                ", isActive=" + isActive +
                '}';
    }
}
