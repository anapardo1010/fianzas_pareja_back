package org.example.app.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.app.service.UserService;
import org.example.app.web.model.ResponseModel;
import org.example.app.web.model.UserCreateModel;
import org.example.app.web.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API para gestionar los usuarios del sistema")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario", description = "Registra un nuevo usuario en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<UserModel>> save(
            @Parameter(description = "Datos del usuario a crear", required = true)
            @Valid @RequestBody UserCreateModel createModel) {
        UserModel user = userService.createUser(createModel);
        return new ResponseEntity<>(ResponseModel.success(user, "Usuario creado exitosamente"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene un usuario específico por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<UserModel>> findById(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id) {
        UserModel user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return ResponseEntity.ok(ResponseModel.success(user, "Usuario encontrado"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error por regla de negocio"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<UserModel>> update(
            @Parameter(description = "ID del usuario a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del usuario", required = true)
            @Valid @RequestBody UserCreateModel updateModel) {
        UserModel user = userService.updateUser(id, updateModel);
        return ResponseEntity.ok(ResponseModel.success(user, "Usuario actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario", description = "Elimina un usuario del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Void>> delete(
            @Parameter(description = "ID del usuario a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ResponseModel.success(null, "Usuario eliminado exitosamente"));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Obtener usuarios por tenant", description = "Obtiene la lista paginada de usuarios de un tenant específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<UserModel>>> findByTenant(
            @Parameter(description = "ID del tenant", required = true, example = "1")
            @PathVariable Long tenantId,
            @Parameter(description = "Número de página (0..N)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Campo por el cual ordenar (ej: name, email, id)")
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
        Page<UserModel> users = userService.findByTenant(tenantId, pageable);
        return ResponseEntity.ok(ResponseModel.success(users, "Usuarios encontrados"));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios", description = "Obtiene la lista paginada de todos los usuarios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios encontrados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ResponseModel<Page<UserModel>>> findAll(
            @Parameter(description = "Número de página (0..N)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Campo por el cual ordenar (ej: name, email, id)")
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
        Page<UserModel> users = userService.findAll(pageable);
        return ResponseEntity.ok(ResponseModel.success(users, "Usuarios encontrados"));
    }
}
