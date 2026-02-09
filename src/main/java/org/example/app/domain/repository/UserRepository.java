package org.example.app.domain.repository;

import org.example.app.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de Users dentro de un tenant.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca usuarios activos por tenant (lista).
     */
    List<User> findByTenant_IdAndIsActiveTrue(Long tenantId);

    /**
     * Busca usuarios activos por tenant (con paginación).
     */
    Page<User> findByTenant_IdAndIsActiveTrue(Long tenantId, Pageable pageable);

    /**
     * Busca un usuario por email y tenant.
     */
    Optional<User> findByEmailAndTenant_IdAndIsActiveTrue(String email, Long tenantId);

    /**
     * Busca un usuario por email global (para validación de unicidad).
     */
    Optional<User> findByEmail(String email);
}
