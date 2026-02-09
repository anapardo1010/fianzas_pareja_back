package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa una categoría personalizada para cada tenant.
 * Cada tenant puede tener sus propias categorías de gastos e ingresos.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_tenant_category", columnList = "id_tenant"),
        @Index(name = "idx_name_category", columnList = "name")
})
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_category")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tenant", nullable = false)
    @Getter @Setter
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    @Getter @Setter
    private String name;

    @Column(name = "description")
    @Getter @Setter
    private String description;

    @Column(name = "is_active", nullable = false)
    @Getter @Setter
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Getter @Setter
    private LocalDateTime updatedAt;

    /**
     * Constructor público para crear una nueva categoría.
     */
    public Category(Tenant tenant, String name, String description, Boolean isActive) {
        this.tenant = tenant;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString con ofuscación para el nombre y descripción.
     */
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) &&
                Objects.equals(name, category.name) &&
                Objects.equals(tenant, category.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, tenant);
    }
}
