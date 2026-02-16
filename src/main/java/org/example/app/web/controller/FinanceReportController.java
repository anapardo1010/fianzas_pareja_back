package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.app.service.FinanceReportService;
import org.example.app.web.model.MonthlyBalanceModel;
import org.example.app.web.model.ProportionalSettlementModel;
import org.example.app.web.model.ResponseModel;
import org.example.app.web.model.UpcomingInstallmentModel;
import org.example.app.web.model.CreditCardBalanceModel;
import org.example.app.web.model.MarkPeriodPaidRequest;
import org.example.app.web.model.CreditCardProportionalPaymentModel;
import org.example.app.web.model.PaymentMethodBalanceModel;
import org.example.app.web.model.PaymentMethodProportionalPaymentModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance-reports")
@RequiredArgsConstructor
@Tag(name = "Finance Reports", description = "API para generar reportes financieros y liquidaciones")
public class FinanceReportController {

    private final FinanceReportService financeReportService;

    @GetMapping("/tenant/{tenantId}/monthly-balance")
    @Operation(summary = "Obtener balance mensual",
            description = "Calcula el balance mensual de un tenant incluyendo ingresos, gastos y cuotas MSI. Puede usar year/month o startDate/endDate.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance mensual calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<MonthlyBalanceModel>> getMonthlyBalance(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Año del reporte (opcional si se usa startDate)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Mes del reporte (1-12, opcional si se usa startDate)", example = "2")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Fecha de inicio del período (opcional si se usa year/month)", example = "2026-02-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del período (opcional)", example = "2026-02-28")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Modo de cálculo: accrual (devengo) o cash (caja). Por defecto accrual.", example = "accrual")
            @RequestParam(required = false, defaultValue = "accrual") String mode) {

        YearMonth yearMonth;

        // Si se proporcionan year y month, usarlos
        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }
        // Si se proporciona startDate, extraer year y month de ella
        else if (startDate != null) {
            yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonthValue());
        }
        // Por defecto, usar el mes actual
        else {
            yearMonth = YearMonth.now();
        }

        MonthlyBalanceModel balance = financeReportService.getMonthlyBalance(tenantId, yearMonth, mode);
        return ResponseEntity.ok(ResponseModel.success(balance, "Balance mensual calculado exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/proportional-settlement")
    @Operation(summary = "Calcular liquidación proporcional",
            description = "Calcula la liquidación proporcional de gastos compartidos entre usuarios de un tenant. Puede usar year/month o startDate/endDate.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liquidación proporcional calculada correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<ProportionalSettlementModel>>> getProportionalSettlement(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Año del reporte (opcional si se usa startDate)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Mes del reporte (1-12, opcional si se usa startDate)", example = "2")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Fecha de inicio del período (opcional si se usa year/month)", example = "2026-02-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del período (opcional)", example = "2026-02-28")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        YearMonth yearMonth;

        // Si se proporcionan year y month, usarlos
        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }
        // Si se proporciona startDate, extraer year y month de ella
        else if (startDate != null) {
            yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonthValue());
        }
        // Por defecto, usar el mes actual
        else {
            yearMonth = YearMonth.now();
        }

        List<ProportionalSettlementModel> settlement = financeReportService.getProportionalSettlement(tenantId, yearMonth);
        return ResponseEntity.ok(ResponseModel.success(settlement, "Liquidación proporcional calculada exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/upcoming-installments")
    @Operation(summary = "Obtener próximas cuotas MSI",
            description = "Obtiene las próximas cuotas a pagar de compras con meses sin intereses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Próximas cuotas obtenidas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<UpcomingInstallmentModel>>> getUpcomingInstallments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Número de meses a futuro para proyectar", example = "3")
            @RequestParam(defaultValue = "3") Integer monthsAhead) {

        List<UpcomingInstallmentModel> installments = financeReportService.getUpcomingInstallments(tenantId, monthsAhead);
        return ResponseEntity.ok(ResponseModel.success(installments, "Próximas cuotas obtenidas exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/overdue-installments")
    @Operation(summary = "Obtener cuotas MSI vencidas",
            description = "Obtiene las cuotas MSI vencidas de un tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuotas vencidas obtenidas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<UpcomingInstallmentModel>>> getOverdueInstallments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {

        List<UpcomingInstallmentModel> installments = financeReportService.getOverdueInstallments(tenantId);
        return ResponseEntity.ok(ResponseModel.success(installments, "Cuotas vencidas obtenidas exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/credit-card-balances")
    @Operation(summary = "Obtener saldos de tarjetas de crédito",
            description = "Calcula el saldo actual de cada tarjeta de crédito considerando fecha de corte, fecha de pago, transacciones y cuotas MSI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldos calculados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<CreditCardBalanceModel>>> getCreditCardBalances(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {

        List<CreditCardBalanceModel> balances = financeReportService.getCreditCardBalances(tenantId);
        return ResponseEntity.ok(ResponseModel.success(balances, "Saldos de tarjetas calculados exitosamente"));
    }

    @PostMapping("/card-balance/{paymentMethodId}/mark-paid")
    @Operation(summary = "Marcar periodo de tarjeta como pagado",
            description = "Marca un periodo específico de una tarjeta de crédito como pagado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Periodo marcado como pagado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Void>> markPeriodAsPaid(
            @Parameter(description = "ID del método de pago (tarjeta)", required = true, example = "1")
            @PathVariable Long paymentMethodId,
            @Parameter(description = "Identificador del periodo a marcar como pagado", required = true)
            @RequestBody MarkPeriodPaidRequest request) {

        financeReportService.markPeriodAsPaid(paymentMethodId, request.getPeriodId());
        return ResponseEntity.ok(ResponseModel.success(null, "Periodo marcado como pagado exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/credit-card-proportional-payments")
    @Operation(summary = "Obtener pagos proporcionales de tarjetas de crédito",
            description = "Calcula cuánto debe pagar cada usuario por cada tarjeta según su porcentaje de contribución en el periodo de corte actual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagos proporcionales calculados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<CreditCardProportionalPaymentModel>>> getCreditCardProportionalPayments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {

        List<CreditCardProportionalPaymentModel> payments = financeReportService.getCreditCardProportionalPayments(tenantId);
        return ResponseEntity.ok(ResponseModel.success(payments, "Pagos proporcionales de tarjetas calculados exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/non-credit-proportional-payments")
    @Operation(summary = "Obtener pagos proporcionales para métodos no-crediticios",
            description = "Calcula cuánto debe pagar cada usuario por cada método que no sea tarjeta de crédito (débito, efectivo, cuentas) en un rango de fechas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagos proporcionales calculados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<PaymentMethodProportionalPaymentModel>>> getNonCreditProportionalPayments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Fecha de inicio del rango (opcional)", example = "2026-02-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del rango (opcional)", example = "2026-02-28")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<PaymentMethodProportionalPaymentModel> payments = financeReportService.getNonCreditPaymentMethodProportionalPayments(tenantId, startDate, endDate);
        return ResponseEntity.ok(ResponseModel.success(payments, "Pagos proporcionales (no-credit) calculados exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}/balance-by-payment-method")
    @Operation(summary = "Obtener balance por método de pago",
            description = "Calcula el balance individual de cada método de pago (efectivo, débito, tarjetas) incluyendo transferencias entrantes y salientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance por método de pago calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<PaymentMethodBalanceModel>>> getBalanceByPaymentMethod(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Año del reporte (opcional)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Mes del reporte (1-12, opcional)", example = "2")
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth;

        // Si se proporcionan year y month, usarlos
        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }
        // Por defecto, usar el mes actual
        else {
            yearMonth = YearMonth.now();
        }

        List<PaymentMethodBalanceModel> balances = financeReportService.getBalanceByPaymentMethod(tenantId, yearMonth);
        return ResponseEntity.ok(ResponseModel.success(balances, "Balance por método de pago calculado exitosamente"));
    }
}
