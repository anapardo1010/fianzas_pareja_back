package org.example.app.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.app.web.model.ResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para capturar errores no controlados.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseModel<Void>> handleException(Exception e) {
        log.error("❌❌❌ ERROR NO CONTROLADO: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseModel.error("Error interno: " + e.getMessage(), "ERR_500"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseModel<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("❌ Error de validación: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseModel.error(e.getMessage(), "ERR_400"));
    }
}

