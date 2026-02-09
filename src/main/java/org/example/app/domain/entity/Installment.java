package org.example.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa una cuota de meses sin intereses (MSI).
 * Permite automatizar el cálculo y seguimiento de pagos diferidos.
 * Autor: [Ana Pardo]
 * Fecha: 05/02/2026
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "installment", indexes = {
        @Index(name = "idx_transaction_installment", columnList = "id_transaction"),
        @Index(name = "idx_projected_date_installment", columnList = "projected_date"),
        @Index(name = "idx_installment_number", columnList = "installment_number")
})
public class Installment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_installment")
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaction", nullable = false)
    @Getter @Setter
    private Transaction transaction;

    @Column(name = "installment_number", nullable = false)
    @Getter @Setter
    private Integer installmentNumber;

    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    @Getter @Setter
    private BigDecimal installmentAmount;

    @Column(name = "projected_date", nullable = false)
    @Getter @Setter
    private LocalDate projectedDate;

    @Column(name = "is_paid", nullable = false)
    @Getter @Setter
    private Boolean isPaid;

    @Column(name = "paid_date")
    @Getter @Setter
    private LocalDate paidDate;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Getter @Setter
    private LocalDateTime updatedAt;

    /**
     * Constructor público para crear una nueva cuota MSI.
     */
    public Installment(Transaction transaction, Integer installmentNumber,
                      BigDecimal installmentAmount, LocalDate projectedDate, Boolean isPaid) {
        this.transaction = transaction;
        this.installmentNumber = installmentNumber;
        this.installmentAmount = installmentAmount;
        this.projectedDate = projectedDate;
        this.isPaid = isPaid;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Método toString sin ofuscación para cantidades y fechas de seguimiento.
     */
    @Override
    public String toString() {
        return "Installment{" +
                "id=" + id +
                ", installmentNumber=" + installmentNumber +
                ", installmentAmount=" + installmentAmount +
                ", projectedDate=" + projectedDate +
                ", isPaid=" + isPaid +
                ", paidDate=" + paidDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Installment)) return false;
        Installment that = (Installment) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(transaction, that.transaction) &&
                Objects.equals(installmentNumber, that.installmentNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transaction, installmentNumber);
    }
}
