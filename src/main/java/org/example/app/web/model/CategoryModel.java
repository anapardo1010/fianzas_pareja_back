package org.example.app.web.model;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.app.domain.entity.Category;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Modelo de respuesta para Category.
 */
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class CategoryModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long tenantId;
    private final String name;
    private final String description;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Función para convertir Entity a Model.
     */
    public static final Function<Category, CategoryModel> FN_ENTITY_TO_MODEL = entity ->
        new CategoryModel(
            entity.getId(),
            entity.getTenant().getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );

    /**
     * Función para convertir Model a Entity (requiere tenant).
     */
    public static Function<CategoryModel, Category> fnModelToEntity(org.example.app.domain.entity.Tenant tenant) {
        return model -> {
            Category entity = new Category(tenant, model.getName(), model.getDescription(), model.getIsActive());
            entity.setId(model.getId());
            entity.setCreatedAt(model.getCreatedAt());
            entity.setUpdatedAt(model.getUpdatedAt());
            return entity;
        };
    }

    @Override
    public String toString() {
        return "CategoryModel{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
