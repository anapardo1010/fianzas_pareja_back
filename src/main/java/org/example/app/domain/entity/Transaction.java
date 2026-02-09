package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa una transacción financiera (Gasto o Ingreso).
 * Mapea los datos provenientes del formulario de finanzas en pareja.
 * Implementa multi-tenancy para separar datos por tenant.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_tenant_transaction", columnList = "id_tenant"),
        @Index(name = "idx_user_transaction", columnList = "id_user"),
        @Index(name = "idx_date_transaction", columnList = "date"),
        @Index(name = "idx_category_transaction", columnList = "id_category"),
        @Index(name = "idx_payment_method_transaction", columnList = "id_payment_method")
})
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaction")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tenant", nullable = false)
    @Getter @Setter
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    @Getter @Setter
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_category")
    @Getter @Setter
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_payment_method")
    @Getter @Setter
    private PaymentMethod paymentMethod;

    @Column(name = "description", nullable = false)
    @Getter @Setter
    private String description;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @Getter @Setter
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    @Getter @Setter
    private LocalDate date;

    @Column(name = "is_shared", nullable = false)
    @Getter @Setter
    private Boolean isShared;

    @Column(name = "transaction_type", nullable = false)
    @Getter @Setter
    private String transactionType; // INCOME, EXPENSE

    @Column(name = "has_installments")
    @Getter @Setter
    private Boolean hasInstallments;

    @Column(name = "total_installments")
    @Getter @Setter
    private Integer totalInstallments;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Getter @Setter
    private LocalDateTime updatedAt;

    /**
     * Constructor público para crear una nueva transacción.
     */
    public Transaction(Tenant tenant, User user, Category category, PaymentMethod paymentMethod,
                      String description, BigDecimal amount, LocalDate date, Boolean isShared,
                      String transactionType, Boolean hasInstallments, Integer totalInstallments) {
        this.tenant = tenant;
        this.user = user;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.isShared = isShared;
        this.transactionType = transactionType;
        this.hasInstallments = hasInstallments;
        this.totalInstallments = totalInstallments;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString con ofuscación para la descripción.
     */
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", description='" + (description != null ? description.replaceAll(".", "*") : null) + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", transactionType='" + transactionType + '\'' +
                ", isShared=" + isShared +
                ", hasInstallments=" + hasInstallments +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(description, that.description) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(date, that.date) &&
                Objects.equals(tenant, that.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, amount, date, tenant);
    }
}