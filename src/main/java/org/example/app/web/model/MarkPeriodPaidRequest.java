package org.example.app.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para marcar un periodo de tarjeta de crédito como pagado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para marcar un periodo de tarjeta como pagado")
public class MarkPeriodPaidRequest {

    @NotBlank(message = "El periodId es obligatorio")
    @Schema(description = "Identificador único del periodo a marcar como pagado",
            example = "2026-01-03_2026-02-02")
    private String periodId;
}
