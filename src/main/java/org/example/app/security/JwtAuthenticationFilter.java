package org.example.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        log.info("=== JWT Filter - Path: {}, Method: {} ===", request.getRequestURI(), request.getMethod());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ No Authorization header or doesn't start with Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.debug("Token extracted: {}", jwt.substring(0, Math.min(20, jwt.length())) + "...");

        try {
            if (jwtService.isTokenValid(jwt)) {
                Long userId = jwtService.extractUserId(jwt);
                Long tenantId = jwtService.extractTenantId(jwt);
                String role = jwtService.extractRole(jwt);

                log.info("✅ Token válido - userId: {}, tenantId: {}, role: {}", userId, tenantId, role);

                // Crear un objeto principal simplificado con los datos del JWT
                JwtPrincipal principal = new JwtPrincipal(userId, tenantId, role);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("✅ Autenticación configurada - Authorities: {}", authToken.getAuthorities());
            } else {
                log.error("❌ Token inválido o expirado");
            }
        } catch (Exception e) {
            log.error("❌ Error al procesar token JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    // Clase interna para representar el principal del JWT
    public static class JwtPrincipal {
        private final Long userId;
        private final Long tenantId;
        private final String role;

        public JwtPrincipal(Long userId, Long tenantId, String role) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public String getRole() {
            return role;
        }
    }
}
