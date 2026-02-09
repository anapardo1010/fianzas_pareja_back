package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.app.service.TenantService;
import org.example.app.web.model.ResponseModel;
import org.example.app.web.model.TenantCreateModel;
import org.example.app.web.model.TenantModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "API para gestionar los tenants del sistema")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @Operation(summary = "Crear un nuevo tenant", description = "Registra un nuevo tenant en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tenant creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<TenantModel>> save(
            @Parameter(description = "Datos del tenant a crear", required = true)
            @Valid @RequestBody TenantCreateModel createModel) {
        TenantModel tenant = tenantService.createTenant(createModel);
        return new ResponseEntity<>(ResponseModel.success(tenant, "Tenant creado exitosamente"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tenant por ID", description = "Obtiene un tenant específico por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<TenantModel>> findById(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long id) {
        TenantModel tenant = tenantService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado"));
        return ResponseEntity.ok(ResponseModel.success(tenant, "Tenant encontrado"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un tenant", description = "Actualiza los datos de un tenant existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<TenantModel>> update(
            @Parameter(description = "ID del tenant a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del tenant", required = true)
            @Valid @RequestBody TenantCreateModel updateModel) {
        TenantModel tenant = tenantService.updateTenant(id, updateModel);
        return ResponseEntity.ok(ResponseModel.success(tenant, "Tenant actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un tenant", description = "Elimina un tenant del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Tenant no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Void>> delete(
            @Parameter(description = "ID del tenant a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ResponseModel.success(null, "Tenant eliminado exitosamente"));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los tenants", description = "Obtiene la lista paginada de todos los tenants")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenants encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<TenantModel>>> findAll(
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
        Page<TenantModel> tenants = tenantService.findAll(pageable);
        return ResponseEntity.ok(ResponseModel.success(tenants, "Tenants encontrados"));
    }

    @GetMapping("/active")
    @Operation(summary = "Obtener tenants activos", description = "Obtiene la lista paginada de tenants activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenants activos encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<TenantModel>>> findAllActive(
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
        Page<TenantModel> tenants = tenantService.findAllActive(pageable);
        return ResponseEntity.ok(ResponseModel.success(tenants, "Tenants activos encontrados"));
    }
}
