package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.Tenant;
import org.example.app.facade.TenantFacade;
import org.example.app.web.model.TenantCreateModel;
import org.example.app.web.model.TenantModel;
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
 * Servicio para la gestión de Tenants (grupos/parejas).
 * Implementa la lógica de negocio para el onboarding y gestión de suscripciones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantFacade tenantFacade;

    /**
     * Crea un nuevo tenant.
     */
    @Transactional
    public TenantModel createTenant(TenantCreateModel createModel) {
        log.info("Iniciando creación de tenant: {}", createModel.getGroupName());

        validateUniqueTenantName(createModel.getGroupName());

        // Crear el tenant
        Tenant tenant = buildTenant(createModel);
        Tenant savedTenant = tenantFacade.save(tenant);
        log.info("Tenant creado con ID: {}", savedTenant.getId());

        return TenantModel.FN_ENTITY_TO_MODEL.apply(savedTenant);
    }

    /**
     * Busca un tenant por ID.
     */
    public Optional<TenantModel> findById(Long id) {
        log.debug("Buscando tenant por ID: {}", id);
        return tenantFacade.findById(id)
                .map(TenantModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca todos los tenants con paginación.
     */
    public Page<TenantModel> findAll(Pageable pageable) {
        log.debug("Buscando todos los tenants con paginación");
        return tenantFacade.findAll(pageable)
                .map(TenantModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca tenants activos con paginación.
     */
    public Page<TenantModel> findAllActive(Pageable pageable) {
        log.debug("Buscando tenants activos con paginación");
        return tenantFacade.findByIsActive(true, pageable)
                .map(TenantModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca tenants activos por tipo de plan.
     */
    public List<TenantModel> findByPlanType(String planType) {
        log.debug("Buscando tenants por tipo de plan: {}", planType);
        return tenantFacade.findByPlanTypeAndActive(planType)
                .stream()
                .map(TenantModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza un tenant.
     */
    @Transactional
    public TenantModel updateTenant(Long tenantId, TenantCreateModel updateModel) {
        log.info("Actualizando tenant: {}", tenantId);

        Tenant tenant = tenantFacade.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));

        // Si el nombre cambió, validar unicidad
        if (!tenant.getGroupName().equals(updateModel.getGroupName())) {
            validateUniqueTenantName(updateModel.getGroupName());
            tenant.setGroupName(updateModel.getGroupName());
        }

        tenant.setPlanType(updateModel.getPlanType());
        // Permitir actualizar el estado activo
        if (updateModel.getIsActive() != null) {
            tenant.setIsActive(updateModel.getIsActive());
        }
        tenant.setUpdatedAt(LocalDateTime.now());

        Tenant updatedTenant = tenantFacade.save(tenant);
        return TenantModel.FN_ENTITY_TO_MODEL.apply(updatedTenant);
    }

    /**
     * Elimina (desactiva) un tenant.
     */
    @Transactional
    public void deleteTenant(Long tenantId) {
        log.info("Eliminando tenant: {}", tenantId);

        Tenant tenant = tenantFacade.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));

        tenant.setIsActive(false);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantFacade.save(tenant);
    }

    /**
     * Obtiene estadísticas básicas de tenants.
     */
    public long getTotalActiveTenants() {
        return tenantFacade.countActiveTenanats();
    }

    // Métodos privados de validación y creación

    private void validateUniqueTenantName(String groupName) {
        if (tenantFacade.existsByGroupName(groupName)) {
            throw new IllegalArgumentException("Ya existe un grupo activo con el nombre: " + groupName);
        }
    }

    private void validateContributionPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0 ||
            percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de aporte debe estar entre 0.01 y 100");
        }
    }

    private Tenant buildTenant(TenantCreateModel createModel) {
        return new Tenant(
            createModel.getGroupName(),
            createModel.getPlanType(),
            true
        );
    }
}
