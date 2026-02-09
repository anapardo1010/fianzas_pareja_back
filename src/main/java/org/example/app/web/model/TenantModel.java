package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.app.domain.entity.Tenant;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Modelo de respuesta para Tenant.
 * Representa la información del tenant en las respuestas de la API.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class TenantModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String groupName;
    private final String planType;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Función para convertir Entity a Model.
     */
    public static final Function<Tenant, TenantModel> FN_ENTITY_TO_MODEL = entity ->
        new TenantModel(
            entity.getId(),
            entity.getGroupName(),
            entity.getPlanType(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );

    /**
     * Función para convertir Model a Entity.
     */
    public static final Function<TenantModel, Tenant> FN_MODEL_TO_ENTITY = model -> {
        Tenant entity = new Tenant(model.getGroupName(), model.getPlanType(), model.getIsActive());
        entity.setId(model.getId());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    };

    @Override
    public String toString() {
        return "TenantModel{" +
                "id=" + id +
                ", groupName='" + (groupName != null ? groupName.replaceAll(".", "*") : null) + '\'' +
                ", planType='" + planType + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}
