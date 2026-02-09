package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que registra los periodos de tarjeta de crédito que han sido marcados como pagados.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Entity
@Table(name = "credit_card_period_payment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_payment_method", "period_id"}),
       indexes = {
           @Index(name = "idx_payment_method_period", columnList = "id_payment_method, period_id")
       })
public class CreditCardPeriodPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_period_payment")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_payment_method", nullable = false)
    @Getter @Setter
    private PaymentMethod paymentMethod;

    @Column(name = "period_id", nullable = false, length = 50)
    @Getter @Setter
    private String periodId; // Formato: "2026-01-03_2026-02-02"

    @Column(name = "period_start", nullable = false)
    @Getter @Setter
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    @Getter @Setter
    private LocalDate periodEnd;

    @Column(name = "paid_date", nullable = false)
    @Getter @Setter
    private LocalDate paidDate;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.paidDate = LocalDate.now();
    }

    /**
     * Constructor para crear un nuevo registro de pago.
     */
    public CreditCardPeriodPayment(PaymentMethod paymentMethod, String periodId,
                                   LocalDate periodStart, LocalDate periodEnd) {
        this.paymentMethod = paymentMethod;
        this.periodId = periodId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
}

