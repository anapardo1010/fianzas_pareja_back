package org.example.app.domain.repository;

import org.example.app.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de Tenants (grupos/parejas).
 * Maneja las operaciones de persistencia para suscripciones.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Busca un tenant por su nombre de grupo activo.
     */
    Optional<Tenant> findByGroupNameAndIsActiveTrue(String groupName);

    /**
     * Busca tenants activos por tipo de plan.
     */
    List<Tenant> findByPlanTypeAndIsActiveTrue(String planType);

    /**
     * Busca tenants por estado activo con paginación.
     */
    Page<Tenant> findByIsActive(boolean isActive, Pageable pageable);

    /**
     * Cuenta el número de tenants activos.
     */
    long countByIsActiveTrue();
}
