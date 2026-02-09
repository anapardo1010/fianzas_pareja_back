package org.example.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Configuración de CORS para permitir peticiones desde el frontend Angular.
 * Permite solicitudes desde http://localhost:4200 (desarrollo).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permitir credenciales (cookies, headers de autorización)
        config.setAllowCredentials(true);

        // Orígenes permitidos (Angular en desarrollo)
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:4201" // Por si usas otro puerto
        ));

        // Headers permitidos
        config.addAllowedHeader("*");

        // Métodos HTTP permitidos
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Headers expuestos (para que el frontend pueda leerlos)
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count"
        ));

        // Aplicar configuración a todas las rutas
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}

