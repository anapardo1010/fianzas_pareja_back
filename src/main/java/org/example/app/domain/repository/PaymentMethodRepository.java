package org.example.app.domain.repository;

import org.example.app.domain.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la gestión de PaymentMethods por usuario.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * Busca métodos de pago activos por usuario.
     */
    List<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Busca métodos de pago por tenant (a través de user) - lista.
     */
    List<PaymentMethod> findByUserTenantIdAndIsActiveTrue(Long tenantId);

    /**
     * Busca métodos de pago por tenant (a través de user) - con paginación.
     */
    Page<PaymentMethod> findByUserTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);
}
