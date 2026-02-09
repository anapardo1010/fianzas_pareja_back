package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.app.service.CategoryService;
import org.example.app.web.model.CategoryCreateModel;
import org.example.app.web.model.CategoryModel;
import org.example.app.web.model.ResponseModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API para gestionar las categorías")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Crear una nueva categoría", description = "Registra una nueva categoría en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<CategoryModel>> save(
            @Parameter(description = "Datos de la categoría a crear", required = true)
            @Valid @RequestBody CategoryCreateModel createModel) {
        CategoryModel category = categoryService.createCategory(createModel);
        return new ResponseEntity<>(ResponseModel.success(category, "Categoría creada exitosamente"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID", description = "Obtiene una categoría específica por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<CategoryModel>> findById(
            @Parameter(description = "ID de la categoría", required = true, example = "1")
            @PathVariable Long id) {
        CategoryModel category = categoryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        return ResponseEntity.ok(ResponseModel.success(category, "Categoría encontrada"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una categoría", description = "Actualiza los datos de una categoría existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<CategoryModel>> update(
            @Parameter(description = "ID de la categoría a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos de la categoría", required = true)
            @Valid @RequestBody CategoryCreateModel updateModel) {
        CategoryModel category = categoryService.updateCategory(id, updateModel);
        return ResponseEntity.ok(ResponseModel.success(category, "Categoría actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una categoría", description = "Elimina una categoría del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Void>> delete(
            @Parameter(description = "ID de la categoría a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ResponseModel.success(null, "Categoría eliminada exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Obtener categorías por tenant", description = "Obtiene la lista paginada de categorías de un tenant específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categorías encontradas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<CategoryModel>>> findByTenant(
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
        Page<CategoryModel> categories = categoryService.findByTenant(tenantId, pageable);
        return ResponseEntity.ok(ResponseModel.success(categories, "Categorías encontradas"));
    }

    @GetMapping
    @Operation(summary = "Obtener todas las categorías", description = "Obtiene la lista paginada de todas las categorías")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categorías encontradas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<CategoryModel>>> findAll(
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
        Page<CategoryModel> categories = categoryService.findAll(pageable);
        return ResponseEntity.ok(ResponseModel.success(categories, "Categorías encontradas"));
    }
}
