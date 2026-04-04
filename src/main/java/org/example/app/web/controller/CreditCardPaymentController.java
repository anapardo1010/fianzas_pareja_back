package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.app.service.CreditCardPaymentService;
import org.example.app.web.model.CreditCardPaymentItemResponse;
import org.example.app.web.model.CreditCardPaymentRequest;
import org.example.app.web.model.ResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-card-payments")
@RequiredArgsConstructor
@Tag(name = "Credit Card Payments", description = "Registro de pagos de tarjetas de crédito")
public class CreditCardPaymentController {

    private final CreditCardPaymentService creditCardPaymentService;

    // -------------------------------------------------------------------------
    // POST — registrar pago
    // -------------------------------------------------------------------------

    @PostMapping("/tenant/{tenantId}/pay")
    @Operation(
            summary = "Registrar pago de tarjeta de crédito",
            description = """
                    Registra cómo se paga una tarjeta de crédito en un periodo dado.
                    Por cada ítem en 'payments' se genera automáticamente una Transaction
                    de tipo CREDIT_PAYMENT en el método de pago de origen (débito, efectivo, etc.).
                    Se permite el pago parcial: no es obligatorio cubrir el totalDue completo en una sola llamada.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o método de origen es tarjeta de crédito"),
            @ApiResponse(responseCode = "404", description = "Tarjeta, método de pago o usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<CreditCardPaymentItemResponse>>> registerPayment(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @RequestBody CreditCardPaymentRequest request) {

        List<CreditCardPaymentItemResponse> result =
                creditCardPaymentService.registerPayment(tenantId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseModel.success(result, "Pago de tarjeta registrado exitosamente"));
    }

    // -------------------------------------------------------------------------
    // GET — historial por tenant
    // -------------------------------------------------------------------------

    @GetMapping("/tenant/{tenantId}/history")
    @Operation(
            summary = "Historial de pagos de tarjetas del tenant",
            description = "Devuelve todos los pagos de tarjetas registrados para el tenant, ordenados por fecha descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<CreditCardPaymentItemResponse>>> getHistoryByTenant(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {

        return ResponseEntity.ok(ResponseModel.success(
                creditCardPaymentService.getHistoryByTenant(tenantId),
                "Historial de pagos obtenido exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // GET — historial por tarjeta
    // -------------------------------------------------------------------------

    @GetMapping("/card/{creditCardId}/history")
    @Operation(
            summary = "Historial de pagos de una tarjeta específica",
            description = "Devuelve todos los pagos registrados para una tarjeta de crédito en todos sus periodos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<CreditCardPaymentItemResponse>>> getHistoryByCard(
            @Parameter(description = "ID de la tarjeta de crédito", required = true, example = "8")
            @PathVariable Long creditCardId) {

        return ResponseEntity.ok(ResponseModel.success(
                creditCardPaymentService.getHistoryByCard(creditCardId),
                "Historial de pagos de tarjeta obtenido exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // GET — detalle de un periodo
    // -------------------------------------------------------------------------

    @GetMapping("/card/{creditCardId}/period/{periodId}")
    @Operation(
            summary = "Pagos de una tarjeta en un periodo específico",
            description = "Devuelve el detalle de cómo se pagó (o se está pagando) una tarjeta en el periodo indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<List<CreditCardPaymentItemResponse>>> getByCardAndPeriod(
            @Parameter(description = "ID de la tarjeta de crédito", required = true, example = "8")
            @PathVariable Long creditCardId,
            @Parameter(description = "Periodo, ej: 2026-03-02_2026-04-01", required = true)
            @PathVariable String periodId) {

        return ResponseEntity.ok(ResponseModel.success(
                creditCardPaymentService.getByCardAndPeriod(creditCardId, periodId),
                "Pagos del periodo obtenidos exitosamente"
        ));
    }

    // -------------------------------------------------------------------------
    // GET — monto ya pagado de un periodo
    // -------------------------------------------------------------------------

    @GetMapping("/card/{creditCardId}/period/{periodId}/paid-amount")
    @Operation(
            summary = "Monto ya pagado de un periodo",
            description = "Devuelve la suma de lo que ya se ha registrado como pagado para una tarjeta en un periodo. " +
                    "Útil para que el front calcule el monto pendiente restante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monto calculado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResponseModel<BigDecimal>> getPaidAmount(
            @Parameter(description = "ID de la tarjeta de crédito", required = true, example = "8")
            @PathVariable Long creditCardId,
            @Parameter(description = "Periodo, ej: 2026-03-02_2026-04-01", required = true)
            @PathVariable String periodId) {

        return ResponseEntity.ok(ResponseModel.success(
                creditCardPaymentService.getPaidAmountByCardAndPeriod(creditCardId, periodId),
                "Monto pagado calculado exitosamente"
        ));
    }
}

