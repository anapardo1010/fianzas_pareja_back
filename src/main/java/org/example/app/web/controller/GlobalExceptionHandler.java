package org.example.app.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.app.web.model.ResponseModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Manejador global de excepciones para capturar errores no controlados.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseModel<Void>> handleNotFound(NoHandlerFoundException e) {
        String path = e.getRequestURL();

        // Ignorar recursos estáticos comunes que no existen
        if (path.contains("favicon.ico") || path.contains(".ico") ||
                path.contains(".png") || path.contains(".jpg") ||
                path.contains(".css") || path.contains(".js")) {
            log.debug("⚠️ Recurso estático no encontrado (ignorado): {}", path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        log.warn("❌ Endpoint no encontrado: {}", path);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseModel.error("Endpoint no encontrado: " + path, "ERR_404"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseModel<Void>> handleException(Exception e) {
        String message = e.getMessage();

        // Ignorar errores de recursos estáticos
        if (message != null && (message.contains("favicon.ico") ||
                message.contains("No static resource"))) {
            log.debug("⚠️ Recurso estático no encontrado (ignorado): {}", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        log.error("❌❌❌ ERROR NO CONTROLADO: {}", message, e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseModel.error("Error interno: " + message, "ERR_500"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseModel<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("❌ Error de validación: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseModel.error(e.getMessage(), "ERR_400"));
    }
}
