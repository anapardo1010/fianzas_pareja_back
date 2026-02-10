package org.example.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;

    // Rutas que NO deben pasar por el filtro de Tenant (son públicas)
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/",
        "/api/v1/auth/",
        "/v3/api-docs",
        "/swagger-ui",
        "/swagger-resources",
        "/webjars/",
        "/favicon.ico",
        "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // No filtrar peticiones OPTIONS (preflight CORS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("⏭️ TenantFilter: Saltando OPTIONS para CORS - {}", path);
            return true;
        }

        // No filtrar rutas públicas (registro, login, swagger)
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            log.debug("⏭️ TenantFilter: Saltando ruta pública - {}", path);
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        log.debug("🏢 TenantFilter - Path: {}", request.getRequestURI());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                if (jwtService.isTokenValid(jwt)) {
                    Long tenantId = jwtService.extractTenantId(jwt);
                    TenantContextHolder.setTenantId(tenantId);
                    log.debug("✅ TenantId configurado: {}", tenantId);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error extrayendo tenantId del token: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
