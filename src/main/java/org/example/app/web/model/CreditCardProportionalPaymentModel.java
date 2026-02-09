package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modelo que representa el pago proporcional de una tarjeta de crédito entre usuarios.
 * Muestra cuánto debe pagar cada usuario según su porcentaje de contribución.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardProportionalPaymentModel {

    private Long paymentMethodId;
    private Long userId; // ID del propietario de la tarjeta
    private String alias; // alias de la tarjeta para identificarla (p.ej. 'Visa personal')
    private String bankName;
    private LocalDate cutDate;
    private LocalDate paymentDate;
    private BigDecimal currentBalance;
    private BigDecimal pendingInstallments;
    private BigDecimal totalDue;
    private String status;
    private String paymentStatus;
    private String periodId;
    private List<UserPaymentShare> userShares;
}
