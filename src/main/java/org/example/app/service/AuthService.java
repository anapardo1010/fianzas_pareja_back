package org.example.app.service;

import lombok.RequiredArgsConstructor;
import org.example.app.domain.entity.User;
import org.example.app.domain.entity.Tenant;
import org.example.app.facade.UserFacade;
import org.example.app.facade.TenantFacade;
import org.example.app.security.JwtService;
import org.example.app.web.model.AuthRequestModel;
import org.example.app.web.model.AuthResponseModel;
import org.example.app.web.model.RegisterRequestModel;
import org.example.app.web.model.RegisterInviteRequestModel;
import org.example.app.web.model.ResponseModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserFacade userFacade;
    private final TenantFacade tenantFacade;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registro de usuario nuevo: crea el tenant automáticamente y el primer usuario como ADMIN.
     * Este es el flujo para cuando un usuario nuevo se registra por primera vez.
     */
    @Transactional
    public ResponseModel<AuthResponseModel> register(RegisterRequestModel request) {
        // Validar que el email no exista
        if (userFacade.existsByEmail(request.getEmail())) {
            return ResponseModel.error("El correo ya está registrado", "FZ_AU_409");
        }

        // Crear tenant automáticamente con el nombre del usuario
        Tenant tenant = new Tenant(
            "Finanzas de " + request.getName(), // groupName
            "FREE",                               // planType (plan gratuito por defecto)
            true                                  // isActive
        );
        tenant = tenantFacade.save(tenant);

        // Crear el usuario como ADMIN (primer usuario del tenant)
        User user = new User(
            tenant,
            request.getName(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            "ADMIN", // El primer usuario siempre es ADMIN
            null,    // contributionPercentage - se puede configurar después
            true
        );

        user = userFacade.save(user);

        // Generar token JWT automáticamente
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tenantId", user.getTenant().getId());
        claims.put("role", user.getRole());

        String token = jwtService.generateToken(claims);
        AuthResponseModel response = new AuthResponseModel(
            token,
            user.getId(),
            user.getTenant().getId(),
            user.getRole()
        );

        return ResponseModel.success(response, "Usuario registrado correctamente. Sesión iniciada.");
    }

    /**
     * Registro de usuario invitado: se une a un tenant existente como USER.
     * Este es el flujo para cuando alguien es invitado a un tenant.
     */
    @Transactional
    public ResponseModel<AuthResponseModel> registerInvite(RegisterInviteRequestModel request) {
        // Validar que el email no exista
        if (userFacade.existsByEmail(request.getEmail())) {
            return ResponseModel.error("El correo ya está registrado", "FZ_AU_409");
        }

        // Validar que el tenant exista
        Tenant tenant = tenantFacade.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado"));

        // Crear el usuario como USER (invitado al tenant)
        User user = new User(
            tenant,
            request.getName(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            "USER", // Usuario invitado es USER
            null,   // contributionPercentage - se puede configurar después
            true
        );

        user = userFacade.save(user);

        // Generar token JWT automáticamente
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tenantId", user.getTenant().getId());
        claims.put("role", user.getRole());

        String token = jwtService.generateToken(claims);
        AuthResponseModel response = new AuthResponseModel(
            token,
            user.getId(),
            user.getTenant().getId(),
            user.getRole()
        );

        return ResponseModel.success(response, "Usuario registrado correctamente. Sesión iniciada.");
    }

    /**
     * Login estándar: valida credenciales y retorna token JWT.
     */
    public ResponseModel<AuthResponseModel> login(AuthRequestModel request) {
        User user = userFacade.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !user.getIsActive() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseModel.error("Credenciales inválidas", "FZ_AU_401");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tenantId", user.getTenant().getId());
        claims.put("role", user.getRole());

        String token = jwtService.generateToken(claims);
        AuthResponseModel response = new AuthResponseModel(token, user.getId(), user.getTenant().getId(), user.getRole());

        return ResponseModel.success(response, "Login exitoso");
    }
}
