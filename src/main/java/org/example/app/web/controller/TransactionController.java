package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.app.service.TransactionService;
import org.example.app.web.model.ResponseModel;
import org.example.app.web.model.TransactionCreateModel;
import org.example.app.web.model.TransactionModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para la gestión de Transactions (Operaciones Core).
 * Maneja el núcleo del sistema: gastos/ingresos, gastos compartidos y MSI.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API para gestionar las transacciones del sistema")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Crear una nueva transacción", description = "Crea una transacción con lógica de gastos compartidos y MSI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transacción creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<TransactionModel>> createTransaction(
            @Parameter(description = "Datos de la transacción a crear", required = true)
            @Valid @RequestBody TransactionCreateModel createModel) {
        TransactionModel transaction = transactionService.createTransaction(createModel);
        return new ResponseEntity<>(ResponseModel.success(transaction, "Transacción creada exitosamente"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener transacción por ID", description = "Obtiene una transacción específica por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacción encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<TransactionModel>> getTransaction(
            @Parameter(description = "ID de la transacción", required = true, example = "1")
            @PathVariable Long id) {
        TransactionModel transaction = transactionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));
        return ResponseEntity.ok(ResponseModel.success(transaction, "Transacción encontrada"));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Obtener transacciones por tenant y rango de fechas",
               description = "Obtiene el historial de transacciones de un tenant en un período específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacciones encontradas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<TransactionModel>>> getTransactionsByTenantAndDateRange(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Fecha de inicio del período", required = true, example = "2026-02-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del período", required = true, example = "2026-02-28")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionModel> transactions = transactionService.findByTenantAndDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(ResponseModel.success(transactions, "Transacciones encontradas"));
    }

    @GetMapping("/tenant/{tenantId}/shared")
    @Operation(summary = "Obtener transacciones compartidas",
               description = "Obtiene las transacciones compartidas de un tenant para cálculos de liquidación proporcional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacciones compartidas encontradas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<TransactionModel>>> getSharedTransactionsByTenantAndDateRange(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Fecha de inicio del período", required = true, example = "2026-02-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin del período", required = true, example = "2026-02-28")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionModel> transactions = transactionService.findSharedByTenantAndDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(ResponseModel.success(transactions, "Transacciones compartidas encontradas"));
    }

    @GetMapping("/tenant/{tenantId}/with-installments")
    @Operation(summary = "Obtener transacciones con MSI",
               description = "Obtiene las transacciones con meses sin intereses de un tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacciones con MSI encontradas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<List<TransactionModel>>> getTransactionsWithInstallments(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId) {
        List<TransactionModel> transactions = transactionService.findWithInstallmentsByTenant(tenantId);
        return ResponseEntity.ok(ResponseModel.success(transactions, "Transacciones con MSI encontradas"));
    }
}
