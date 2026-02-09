package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un usuario dentro de un tenant.
 * Varios usuarios pueden pertenecer a un solo tenant.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "usuario", indexes = {
        @Index(name = "idx_tenant_user", columnList = "id_tenant"),
        @Index(name = "idx_email_user", columnList = "email")
})
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tenant", nullable = false)
    @Getter @Setter
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    @Getter @Setter
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    @Getter @Setter
    private String email;

    @Column(name = "password", nullable = false)
    @Getter @Setter
    private String password;

    @Column(name = "role", nullable = false)
    @Getter @Setter
    private String role; // ADMIN o USER

    @Column(name = "contribution_percentage", precision = 5, scale = 2)
    @Getter @Setter
    private BigDecimal contributionPercentage;

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
     * Constructor público para crear un nuevo usuario.
     */
    public User(Tenant tenant, String name, String email,
                String password, String role, BigDecimal contributionPercentage, Boolean isActive) {
        this.tenant = tenant;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.contributionPercentage = contributionPercentage;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString con ofuscación para el nombre y email.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + (name != null ? name.replaceAll(".", "*") : null) + '\'' +
                ", email='" + (email != null ? email.replaceAll(".", "*") : null) + '\'' +
                ", contributionPercentage=" + contributionPercentage +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(email, user.email) &&
                Objects.equals(tenant, user.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, tenant);
    }
}
