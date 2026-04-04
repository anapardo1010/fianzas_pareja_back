package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registra cómo se paga cada tarjeta de crédito en un periodo.
 * Un pago puede dividirse en varias filas (distintos métodos de origen).
 * Cada fila genera una Transaction de tipo CREDIT_PAYMENT automáticamente.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "credit_card_payment", indexes = {
        @Index(name = "idx_ccp_tenant",      columnList = "id_tenant"),
        @Index(name = "idx_ccp_credit_card", columnList = "id_credit_card"),
        @Index(name = "idx_ccp_period",      columnList = "id_credit_card, billing_period_id"),
        @Index(name = "idx_ccp_source_pm",   columnList = "id_source_payment_method")
})
public class CreditCardPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credit_card_payment")
    @Getter @Setter
    private Long id;

    @Column(name = "id_tenant", nullable = false)
    @Getter @Setter
    private Long tenantId;

    /** Tarjeta de crédito que se está pagando. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_credit_card", nullable = false)
    @Getter @Setter
    private PaymentMethod creditCard;

    /** Periodo de facturación, ej: "2026-03-02_2026-04-01". */
    @Column(name = "billing_period_id", nullable = false, length = 50)
    @Getter @Setter
    private String billingPeriodId;

    /** Método de pago de origen (débito, efectivo, etc.). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_source_payment_method", nullable = false)
    @Getter @Setter
    private PaymentMethod sourcePaymentMethod;

    /** Transaction CREDIT_PAYMENT generada automáticamente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaction")
    @Getter @Setter
    private Transaction transaction;

    /** Usuario que realizó / registró el pago. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paid_by_user", nullable = false)
    @Getter @Setter
    private User paidByUser;

    /** Monto pagado con este método de origen. */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Getter @Setter
    private BigDecimal amount;

    @Column(name = "notes")
    @Getter @Setter
    private String notes;

    @Column(name = "paid_at", nullable = false)
    @Getter @Setter
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.paidAt    == null) this.paidAt    = LocalDateTime.now();
    }

    /** Constructor de negocio. La transaction se asigna después de persistirse. */
    public CreditCardPayment(Long tenantId, PaymentMethod creditCard, String billingPeriodId,
                             PaymentMethod sourcePaymentMethod, User paidByUser,
                             BigDecimal amount, String notes) {
        this.tenantId            = tenantId;
        this.creditCard          = creditCard;
        this.billingPeriodId     = billingPeriodId;
        this.sourcePaymentMethod = sourcePaymentMethod;
        this.paidByUser          = paidByUser;
        this.amount              = amount;
        this.notes               = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreditCardPayment)) return false;
        CreditCardPayment that = (CreditCardPayment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

