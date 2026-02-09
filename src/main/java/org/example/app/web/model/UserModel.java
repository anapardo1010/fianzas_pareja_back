package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.app.domain.entity.User;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Modelo de respuesta para User.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class UserModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long tenantId;
    private final String name;
    private final String email;
    private final BigDecimal contributionPercentage;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Función para convertir Entity a Model.
     */
    public static final Function<User, UserModel> FN_ENTITY_TO_MODEL = entity ->
        new UserModel(
            entity.getId(),
            entity.getTenant().getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getContributionPercentage(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );

    /**
     * Función para convertir Model a Entity (requiere tenant).
     */
    public static Function<UserModel, User> fnModelToEntity(org.example.app.domain.entity.Tenant tenant) {
        return model -> {
            User entity = new User(
                tenant,
                model.getName(),
                model.getEmail(),
                null, // password - será null para modelos sin autenticación
                "USER", // role por defecto
                model.getContributionPercentage(),
                model.getIsActive()
            );
            entity.setId(model.getId());
            entity.setCreatedAt(model.getCreatedAt());
            entity.setUpdatedAt(model.getUpdatedAt());
            return entity;
        };
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", email='" + (email != null ? email.replaceAll(".", "*") : null) + '\'' +
                ", contributionPercentage=" + contributionPercentage +
                ", isActive=" + isActive +
                '}';
    }
}
