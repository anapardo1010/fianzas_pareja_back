package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modelo que representa el pago proporcional de un método de pago no-crediticio
 * (débito, efectivo, cuentas) entre usuarios en un rango de fechas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodProportionalPaymentModel {

    private Long paymentMethodId;
    private Long userId; // propietario del método
    private String alias;
    private String bankName;
    private String accountType;
    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private BigDecimal currentBalance; // suma neta de transacciones en el rango
    private int transactionCount;
    private List<UserPaymentShare> userShares;
}

