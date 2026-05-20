package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Modelo que representa un periodo/corte específico de una tarjeta de crédito.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardPeriodModel {
    private String periodId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paymentDate;
    private String paymentStatus; // PAID, PENDING, OVERDUE
    private boolean paid;
}
