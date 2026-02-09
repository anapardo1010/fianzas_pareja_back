package org.example.app.domain.repository;

import org.example.app.domain.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de Categories por tenant.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Busca categorías activas por tenant (lista).
     */
    List<Category> findByTenant_IdAndIsActiveTrue(Long tenantId);

    /**
     * Busca categorías activas por tenant (con paginación).
     */
    Page<Category> findByTenant_IdAndIsActiveTrue(Long tenantId, Pageable pageable);

    /**
     * Busca una categoría por nombre y tenant.
     */
    Optional<Category> findByNameAndTenant_IdAndIsActiveTrue(String name, Long tenantId);
}
