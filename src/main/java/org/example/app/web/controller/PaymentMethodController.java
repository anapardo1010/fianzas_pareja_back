package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.app.service.PaymentMethodService;
import org.example.app.web.model.PaymentMethodCreateModel;
import org.example.app.web.model.PaymentMethodModel;
import org.example.app.web.model.ResponseModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "API para gestionar los métodos de pago")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @Operation(summary = "Crear un nuevo método de pago", description = "Registra un nuevo método de pago en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Método de pago creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<PaymentMethodModel>> save(
            @Parameter(description = "Datos del método de pago a crear", required = true)
            @Valid @RequestBody PaymentMethodCreateModel createModel) {
        PaymentMethodModel paymentMethod = paymentMethodService.createPaymentMethod(createModel);
        return new ResponseEntity<>(ResponseModel.success(paymentMethod, "Método de pago creado exitosamente"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener método de pago por ID", description = "Obtiene un método de pago específico por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Método de pago encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<PaymentMethodModel>> findById(
            @Parameter(description = "ID del método de pago", required = true, example = "1")
            @PathVariable Long id) {
        PaymentMethodModel paymentMethod = paymentMethodService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado"));
        return ResponseEntity.ok(ResponseModel.success(paymentMethod, "Método de pago encontrado"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un método de pago", description = "Actualiza los datos de un método de pago existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Método de pago actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<PaymentMethodModel>> update(
            @Parameter(description = "ID del método de pago a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del método de pago", required = true)
            @Valid @RequestBody PaymentMethodCreateModel updateModel) {
        PaymentMethodModel paymentMethod = paymentMethodService.updatePaymentMethod(id, updateModel);
        return ResponseEntity.ok(ResponseModel.success(paymentMethod, "Método de pago actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un método de pago", description = "Elimina un método de pago del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Método de pago eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Void>> delete(
            @Parameter(description = "ID del método de pago a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.ok(ResponseModel.success(null, "Método de pago eliminado exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Obtener métodos de pago por tenant", description = "Obtiene la lista paginada de métodos de pago de un tenant específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métodos de pago encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<PaymentMethodModel>>> findByTenant(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Número de página (0..N)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Campo por el cual ordenar (ej: name, id)")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección del ordenamiento (ASC o DESC)")
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            sortDirection = Sort.Direction.DESC;
        }

        var pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<PaymentMethodModel> paymentMethods = paymentMethodService.findByTenant(tenantId, pageable);
        return ResponseEntity.ok(ResponseModel.success(paymentMethods, "Métodos de pago encontrados"));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los métodos de pago", description = "Obtiene la lista paginada de todos los métodos de pago")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métodos de pago encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<PaymentMethodModel>>> findAll(
            @Parameter(description = "Número de página (0..N)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Campo por el cual ordenar (ej: name, id)")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección del ordenamiento (ASC o DESC)")
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            sortDirection = Sort.Direction.DESC;
        }

        var pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<PaymentMethodModel> paymentMethods = paymentMethodService.findAll(pageable);
        return ResponseEntity.ok(ResponseModel.success(paymentMethods, "Métodos de pago encontrados"));
    }
}
