package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un método de pago (tarjeta de crédito/débito, cuenta bancaria, etc.)
 * Cada método de pago pertenece a un usuario específico.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment_method", indexes = {
        @Index(name = "idx_user_payment_method", columnList = "id_user"),
        @Index(name = "idx_bank_name_payment_method", columnList = "bank_name")
})
public class PaymentMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_payment_method")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    @Getter @Setter
    private User user;

    @Column(name = "bank_name", nullable = false)
    @Getter @Setter
    private String bankName;

    @Column(name = "alias")
    @Getter @Setter
    private String alias; // alias opcional para identificar la tarjeta (p.ej. "Visa personal")

    @Column(name = "account_type")
    @Getter @Setter
    private String accountType; // CREDIT, DEBIT, CASH

    @Column(name = "cut_day")
    @Getter @Setter
    private Integer cutDay;

    @Column(name = "payment_day")
    @Getter @Setter
    private Integer paymentDay;

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
     * Constructor público para crear un nuevo método de pago.
     */
    public PaymentMethod(User user, String bankName, String accountType,
                        Integer cutDay, Integer paymentDay, Boolean isActive) {
        this.user = user;
        this.bankName = bankName;
        this.accountType = accountType;
        this.cutDay = cutDay;
        this.paymentDay = paymentDay;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString con ofuscación para el nombre del banco.
     */
    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id=" + id +
                ", bankName='" + (bankName != null ? bankName.replaceAll(".", "*") : null) + '\'' +
                ", accountType='" + accountType + '\'' +
                ", cutDay=" + cutDay +
                ", paymentDay=" + paymentDay +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentMethod)) return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(bankName, that.bankName) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankName, user);
    }
}
