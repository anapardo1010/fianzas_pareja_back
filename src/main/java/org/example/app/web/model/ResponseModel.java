package org.example.app.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Modelo estándar de respuesta para las APIs del sistema.
 * Utiliza un builder para facilitar la construcción y asegurar la trazabilidad y consistencia.
 *
 * Ejemplo de uso:
 * ResponseModel.builder().businessCode(...).message(...).data(...).traceId(...).build();
 */
public final class ResponseModel<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Código de negocio con la nomenclatura FZ_YY_###
     * Ejemplo: FZ_TS_200
     * Permite identificar el tipo de respuesta y su origen.
     */
    private String businessCode;

    /**
     * Mensaje de negocio asociado al código.
     * Ejemplo: "Operación realizada exitosamente".
     * Proporciona información legible para el usuario o desarrollador.
     */
    private String message;

    /**
     * Identificador único de la traza (traceId) para seguimiento de la petición.
     * Útil para depuración y trazabilidad en sistemas distribuidos.
     */
    private String traceId;

    /**
     * Datos del recurso consultado, creado o modificado.
     * Puede ser un objeto, una lista o una página de resultados.
     */
    private T data;

    /**
     * Metadatos adicionales, como información de paginación.
     * Se rellena automáticamente si el dato es una página.
     */
    private Metadata metadata;

    // Constructor privado para forzar el uso del builder
    private ResponseModel() {}

    // Getters
    public String getBusinessCode() { return businessCode; }
    public String getMessage() { return message; }
    public String getTraceId() { return traceId; }
    public T getData() { return data; }
    public Metadata getMetadata() { return metadata; }

    /**
     * Clase interna para metadatos de la respuesta, como información de paginación.
     * Se utiliza principalmente cuando la respuesta contiene una lista paginada.
     */
    public static class Metadata implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Número de la página actual (empezando en 0) */
        private int page;
        /** Tamaño de la página (cantidad de elementos por página) */
        private int size;
        /** Total de elementos disponibles en la consulta */
        private long elements;

        public Metadata() {}
        public Metadata(int page, int size, long elements) {
            this.page = page;
            this.size = size;
            this.elements = elements;
        }

        // Getters
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getElements() { return elements; }
    }

    // ============================ BUILDER =========================== //

    /**
     * Devuelve un builder para construir instancias de ResponseModel.
     */
    public static <T> ResponseModelBuilder<T> builder() {
        return new ResponseModelBuilder<>();
    }

    /**
     * Builder para ResponseModel. Permite construir la respuesta paso a paso.
     */
    public static class ResponseModelBuilder<T> {
        private final ResponseModel<T> instance;

        private ResponseModelBuilder() {
            instance = new ResponseModel<>();
        }

        public ResponseModelBuilder<T> businessCode(String businessCode) {
            instance.businessCode = businessCode;
            return this;
        }

        public ResponseModelBuilder<T> message(String message) {
            instance.message = message;
            return this;
        }

        public ResponseModelBuilder<T> traceId(String traceId) {
            instance.traceId = traceId;
            return this;
        }

        public ResponseModelBuilder<T> data(T data) {
            if (Objects.isNull(data)) {
                instance.data = null;
                instance.metadata = new Metadata(0, 0, 0);
            } else if (data instanceof org.springframework.data.domain.Page<?> page) {
                instance.data = (T) page.getContent();
                instance.metadata = new Metadata(page.getNumber(), page.getSize(), page.getTotalElements());
            } else {
                instance.data = data;
                instance.metadata = new Metadata(0, 0, 1);
            }
            return this;
        }

        public ResponseModel<T> build() {
            return instance;
        }
    }

    // Métodos de conveniencia para mantener compatibilidad
    public static <T> ResponseModel<T> success(T data, String message) {
        return ResponseModel.<T>builder()
                .businessCode("FZ_SC_200")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseModel<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    public static <T> ResponseModel<T> error(String message, String errorCode) {
        return ResponseModel.<T>builder()
                .businessCode(errorCode)
                .message(message)
                .data(null)
                .build();
    }

    @Override
    public String toString() {
        return "ResponseModel{" +
                "businessCode='" + businessCode + '\'' +
                ", message='" + message + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
