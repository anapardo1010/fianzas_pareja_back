package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.Tenant;
import org.example.app.domain.entity.User;
import org.example.app.facade.TenantFacade;
import org.example.app.facade.UserFacade;
import org.example.app.web.model.UserCreateModel;
import org.example.app.web.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de Users dentro de tenants.
 * Implementa la lógica de negocio para usuarios y sus porcentajes de aporte.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserFacade userFacade;
    private final TenantFacade tenantFacade;

    /**
     * Crea un nuevo usuario dentro de un tenant.
     */
    @Transactional
    public UserModel createUser(UserCreateModel createModel) {
        log.info("Creando usuario: {} para tenant: {}", createModel.getEmail(), createModel.getTenantId());

        // Validar que el tenant existe
        Tenant tenant = tenantFacade.findById(createModel.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + createModel.getTenantId()));

        // Validar email único
        validateUniqueEmail(createModel.getEmail());

        // Validar porcentaje de aporte
        validateContributionPercentage(createModel.getContributionPercentage());

        // Validar que la suma de porcentajes no exceda 100%
        validateTotalContributionPercentage(createModel.getTenantId(), createModel.getContributionPercentage());

        // Crear usuario con valores por defecto para password y role
        User user = new User(
            tenant,
            createModel.getName(),
            createModel.getEmail(),
            null, // password - será null para usuarios creados sin autenticación
            "USER", // role por defecto
            createModel.getContributionPercentage(),
            true
        );

        User savedUser = userFacade.save(user);
        log.info("Usuario creado con ID: {}", savedUser.getId());

        return UserModel.FN_ENTITY_TO_MODEL.apply(savedUser);
    }

    /**
     * Busca usuarios activos por tenant (lista).
     */
    public List<UserModel> findByTenantList(Long tenantId) {
        log.debug("Buscando usuarios para tenant: {}", tenantId);
        return userFacade.findByTenantIdAndActive(tenantId)
                .stream()
                .map(UserModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Busca usuarios activos por tenant con paginación.
     */
    public Page<UserModel> findByTenant(Long tenantId, Pageable pageable) {
        log.debug("Buscando usuarios para tenant: {} con paginación", tenantId);
        return userFacade.findByTenantIdAndActive(tenantId, pageable)
                .map(UserModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca todos los usuarios con paginación.
     */
    public Page<UserModel> findAll(Pageable pageable) {
        log.debug("Buscando todos los usuarios con paginación");
        return userFacade.findAll(pageable)
                .map(UserModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca un usuario por ID.
     */
    public Optional<UserModel> findById(Long id) {
        log.debug("Buscando usuario por ID: {}", id);
        return userFacade.findById(id)
                .map(UserModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Actualiza el porcentaje de aporte de un usuario.
     */
    @Transactional
    public UserModel updateContributionPercentage(Long userId, BigDecimal newPercentage) {
        log.info("Actualizando porcentaje de aporte del usuario {} a {}", userId, newPercentage);

        User user = userFacade.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Calcular diferencia para validación
        BigDecimal currentPercentage = user.getContributionPercentage();
        BigDecimal difference = newPercentage.subtract(currentPercentage);

        // Validar que el nuevo total no exceda 100%
        validateTotalContributionPercentageUpdate(user.getTenant().getId(), userId, difference);

        user.setContributionPercentage(newPercentage);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userFacade.save(user);
        return UserModel.FN_ENTITY_TO_MODEL.apply(updatedUser);
    }

    /**
     * Actualiza un usuario.
     */
    @Transactional
    public UserModel updateUser(Long userId, UserCreateModel updateModel) {
        log.info("Actualizando usuario: {}", userId);

        User user = userFacade.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Si el email cambió, validar unicidad
        if (!user.getEmail().equals(updateModel.getEmail())) {
            validateUniqueEmail(updateModel.getEmail());
            user.setEmail(updateModel.getEmail());
        }

        // Validar el nuevo porcentaje
        BigDecimal newPercentage = updateModel.getContributionPercentage();
        if (newPercentage != null) {
            validateContributionPercentage(newPercentage);

            // Calcular diferencia de porcentaje (manejar null en currentPercentage)
            BigDecimal currentPercentage = user.getContributionPercentage();
            if (currentPercentage == null) {
                currentPercentage = BigDecimal.ZERO;
            }
            BigDecimal difference = newPercentage.subtract(currentPercentage);

            // Validar que el nuevo total no exceda 100%
            validateTotalContributionPercentageUpdate(user.getTenant().getId(), userId, difference);

            user.setContributionPercentage(newPercentage);
        }

        user.setName(updateModel.getName());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userFacade.save(user);
        return UserModel.FN_ENTITY_TO_MODEL.apply(updatedUser);
    }

    /**
     * Desactiva un usuario.
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Desactivando usuario: {}", userId);

        User user = userFacade.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userFacade.save(user);
    }

    /**
     * Elimina (desactiva) un usuario.
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Eliminando usuario: {}", userId);

        User user = userFacade.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userFacade.save(user);
    }

    // Métodos privados de validación

    private void validateUniqueEmail(String email) {
        if (userFacade.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }
    }

    private void validateContributionPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0 ||
            percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de aporte debe estar entre 0.01 y 100");
        }
    }

    private void validateTotalContributionPercentage(Long tenantId, BigDecimal newPercentage) {
        List<User> existingUsers = userFacade.findByTenantIdAndActive(tenantId);

        BigDecimal totalPercentage = existingUsers.stream()
                .map(user -> user.getContributionPercentage() != null ? user.getContributionPercentage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.add(newPercentage).compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("La suma de porcentajes de aporte no puede exceder 100%");
        }
    }

    private void validateTotalContributionPercentageUpdate(Long tenantId, Long excludeUserId, BigDecimal difference) {
        List<User> existingUsers = userFacade.findByTenantIdAndActive(tenantId);

        // Calcular el total actual excluyendo al usuario que se está actualizando
        BigDecimal totalPercentage = existingUsers.stream()
                .filter(user -> !user.getId().equals(excludeUserId))
                .map(user -> user.getContributionPercentage() != null ? user.getContributionPercentage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agregar la diferencia (nuevo porcentaje del usuario a actualizar)
        BigDecimal newTotal = totalPercentage.add(difference);

        log.debug("Validación de porcentajes - Total actual (sin usuario {}): {}, Diferencia: {}, Nuevo total: {}",
                  excludeUserId, totalPercentage, difference, newTotal);

        // Solo validar si el nuevo total es mayor a 100
        // Permitir que sea menor (los usuarios pueden no sumar 100% todavía)
        if (newTotal.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                String.format("La suma de porcentajes de aporte no puede exceder 100%%. " +
                             "Total actual de otros usuarios: %s%%, Nuevo porcentaje: %s%%, Total resultante: %s%%",
                             totalPercentage, difference, newTotal)
            );
        }
    }
}
