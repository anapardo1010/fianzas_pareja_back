package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.app.service.FinanceReportService;
import org.example.app.web.model.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance-reports")
@RequiredArgsConstructor
@Tag(name = "Finance Reports", description = "Reportes financieros y liquidaciones")
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    @GetMapping("/tenant/{tenantId}/monthly-balance")
    @Operation(
            summary = "Balance mensual",
            description = "Calcula ingresos vs gastos de un tenant en el periodo indicado. " +
                    "Modo accrual (por defecto): gastos se cuentan cuando ocurren. " +
                    "Modo cash: solo salidas reales de efectivo/débito."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<MonthlyBalanceModel>> getMonthlyBalance(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Año (opcional si se usa startDate)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Mes 1-12 (opcional si se usa startDate)", example = "2")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Fecha de inicio (opcional)", example = "2026-02-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Modo de cálculo: accrual | cash", example = "accrual")
            @RequestParam(required = false, defaultValue = "accrual") String mode) {

        YearMonth yearMonth = resolveYearMonth(year, month, startDate);
        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getMonthlyBalance(tenantId, yearMonth, mode),
                "Balance mensual calculado exitosamente"
        ));
    }

    @GetMapping("/tenant/{tenantId}/monthly-balances")
    @Operation(
            summary = "Balances mensuales",
            description = "Devuelve una lista de balances mensuales para el tenant. Por defecto devuelve los últimos 6 meses. " +
                    "Se puede especificar el número de meses y el modo (accrual|cash)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balances calculados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<MonthlyBalanceModel>>> getMonthlyBalances(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Número de meses a devolver (por defecto 6)", example = "6")
            @RequestParam(required = false, defaultValue = "6") Integer months,
            @Parameter(description = "Modo de cálculo: accrual | cash", example = "accrual")
            @RequestParam(required = false, defaultValue = "accrual") String mode) {

        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getMonthlyBalances(tenantId, months, mode),
                "Balances mensuales calculados exitosamente"
        ));
    }

    @GetMapping("/tenant/{tenantId}/balance-by-payment-method")
    @Operation(
            summary = "Balance por método de pago",
            description = "Calcula ingresos, gastos y transferencias de cada método de pago " +
                    "del tenant en el mes indicado. Usado en el dashboard para onboarding."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<PaymentMethodBalanceModel>>> getBalanceByPaymentMethod(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Año (opcional, default: año actual)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Mes 1-12 (opcional, default: mes actual)", example = "2")
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth = resolveYearMonth(year, month, null);
        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getBalanceByPaymentMethod(tenantId, yearMonth),
                "Balance por método de pago calculado exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // Tarjetas de crédito
    // -------------------------------------------------------------------------

    @GetMapping("/tenant/{tenantId}/credit-card-proportional-payments")
    @Operation(
            summary = "Pagos proporcionales por tarjeta de crédito",
            description = "Calcula cuánto debe pagar cada usuario por cada tarjeta según su porcentaje " +
                    "de contribución. Incluye transacciones del ciclo actual y cuotas MSI."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<CreditCardProportionalPaymentModel>>> getCreditCardProportionalPayments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {

        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getCreditCardProportionalPayments(tenantId),
                "Pagos proporcionales de tarjetas calculados exitosamente"
        ));
    }

    @PostMapping("/card-balance/{paymentMethodId}/mark-paid")
    @Operation(
            summary = "Marcar periodo de tarjeta como pagado",
            description = "Registra un periodo de corte de tarjeta de crédito como pagado " +
                    "para que el sistema avance al siguiente ciclo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Periodo marcado como pagado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<Void>> markPeriodAsPaid(
            @Parameter(description = "ID del método de pago (tarjeta)", required = true, example = "1")
            @PathVariable Long paymentMethodId,
            @RequestBody MarkPeriodPaidRequest request) {

        financeReportService.markPeriodAsPaid(paymentMethodId, request.getPeriodId());
        return ResponseEntity.ok(ResponseModel.success(null, "Periodo marcado como pagado exitosamente"));
    }

    // -------------------------------------------------------------------------
    // MSI (Meses Sin Intereses)
    // -------------------------------------------------------------------------

    @GetMapping("/tenant/{tenantId}/upcoming-installments")
    @Operation(
            summary = "Próximas cuotas MSI",
            description = "Lista las cuotas de compras a meses sin intereses que vencen " +
                    "en los próximos N meses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuotas obtenidas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<UpcomingInstallmentModel>>> getUpcomingInstallments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Meses a proyectar hacia adelante (por defecto 3)", example = "3")
            @RequestParam(defaultValue = "3") Integer monthsAhead) {

        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getUpcomingInstallments(tenantId, monthsAhead),
                "Próximas cuotas obtenidas exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // Débito y efectivo
    // -------------------------------------------------------------------------

    @GetMapping("/tenant/{tenantId}/non-credit-proportional-payments")
    @Operation(
            summary = "Pagos proporcionales para débito y efectivo",
            description = "Calcula cuánto debe asumir cada usuario por los gastos realizados " +
                    "con métodos no crediticios (débito, efectivo) en el rango indicado. " +
                    "Si no se especifica rango, usa el mes actual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<PaymentMethodProportionalPaymentModel>>> getNonCreditProportionalPayments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Fecha de inicio del rango (opcional)", example = "2026-02-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del rango (opcional)", example = "2026-02-28")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ResponseModel.success(
                financeReportService.getNonCreditPaymentMethodProportionalPayments(tenantId, startDate, endDate),
                "Pagos proporcionales (no-credit) calculados exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // Helper privado
    // -------------------------------------------------------------------------

    /**
     * Resuelve el YearMonth a partir de year+month, startDate, o el mes actual (en ese orden).
     */
    private YearMonth resolveYearMonth(Integer year, Integer month, LocalDate startDate) {
        if (year != null && month != null) return YearMonth.of(year, month);
        if (startDate != null) return YearMonth.of(startDate.getYear(), startDate.getMonthValue());
        return YearMonth.now();
    }

}