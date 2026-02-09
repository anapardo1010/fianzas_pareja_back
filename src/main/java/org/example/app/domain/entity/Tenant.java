package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un tenant (grupo/pareja) en el sistema multi-tenant.
 * Cada tenant representa una suscripción independiente.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tenant", indexes = {
        @Index(name = "idx_plan_type_tenant", columnList = "plan_type"),
        @Index(name = "idx_active_tenant", columnList = "is_active")
})
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tenant")
    @Getter @Setter
    private Long id;

    @Column(name = "group_name", nullable = false)
    @Getter @Setter
    private String groupName;

    @Column(name = "plan_type", nullable = false)
    @Getter @Setter
    private String planType; // FREE, PREMIUM, PRO

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
     * Constructor público para crear un nuevo tenant.
     */
    public Tenant(String groupName, String planType, Boolean isActive) {
        this.groupName = groupName;
        this.planType = planType;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString con ofuscación para el nombre del grupo.
     */
    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", groupName='" + (groupName != null ? groupName.replaceAll(".", "*") : null) + '\'' +
                ", planType='" + planType + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant)) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(id, tenant.id) &&
                Objects.equals(groupName, tenant.groupName) &&
                Objects.equals(planType, tenant.planType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupName, planType);
    }
}
