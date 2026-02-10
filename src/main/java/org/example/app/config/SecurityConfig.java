package org.example.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.security.JwtAuthenticationFilter;
import org.example.app.security.TenantContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantContextFilter tenantContextFilter;

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4201}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // Usar orígenes desde variable de entorno
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.forEach(origin -> {
            String trimmedOrigin = origin.trim();
            config.addAllowedOrigin(trimmedOrigin);
            log.info("✅ CORS: Origen permitido -> {}", trimmedOrigin);
        });

        // Permitir todos los headers (importante para preflight)
        config.addAllowedHeader("*");

        // Métodos permitidos (incluyendo OPTIONS para preflight)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers expuestos
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));

        // Max age para preflight cache (1 hora)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔧 Configurando SecurityFilterChain...");

        http
            // CORS DEBE IR PRIMERO
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Permitir TODAS las peticiones OPTIONS sin autenticación (CRÍTICO para CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Endpoints públicos (sin autenticación)
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                // Swagger UI y API Docs (IMPORTANTE: sin autenticación para que funcione en Render)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                // Todos los demás endpoints requieren autenticación
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.error("❌ 401 Unauthorized - Path: {}, Error: {}", request.getRequestURI(), authException.getMessage());
                    response.sendError(401, "No autorizado");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.error("❌ 403 Forbidden - Path: {}, Error: {}", request.getRequestURI(), accessDeniedException.getMessage());
                    log.error("❌ Authentication: {}", org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
                    response.sendError(403, "Acceso denegado");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tenantContextFilter, JwtAuthenticationFilter.class);

        log.info("✅ SecurityFilterChain configurado correctamente");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
