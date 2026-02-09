package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.PaymentMethod;
import org.example.app.domain.entity.User;
import org.example.app.facade.PaymentMethodFacade;
import org.example.app.facade.UserFacade;
import org.example.app.web.model.PaymentMethodCreateModel;
import org.example.app.web.model.PaymentMethodModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de PaymentMethods (métodos de pago/tarjetas).
 * Implementa la lógica de negocio para tarjetas de crédito/débito y cuentas bancarias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodFacade paymentMethodFacade;
    private final UserFacade userFacade;

    /**
     * Crea un nuevo método de pago para un usuario.
     */
    @Transactional
    public PaymentMethodModel createPaymentMethod(PaymentMethodCreateModel createModel) {
        log.info("Creando método de pago: {} para usuario: {}", createModel.getBankName(), createModel.getUserId());

        // Validar que el usuario existe
        User user = userFacade.findById(createModel.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + createModel.getUserId()));

        // Validar días de corte y pago para tarjetas de crédito
        validateCreditCardDays(createModel);

        // Crear método de pago
        PaymentMethod paymentMethod = new PaymentMethod(
            user,
            createModel.getBankName(),
            createModel.getAccountType(),
            createModel.getCutDay(),
            createModel.getPaymentDay(),
            true
        );
        // asignar alias si viene en el modelo
        paymentMethod.setAlias(createModel.getAlias());

        PaymentMethod savedPaymentMethod = paymentMethodFacade.save(paymentMethod);
        log.info("Método de pago creado con ID: {}", savedPaymentMethod.getId());

        return PaymentMethodModel.FN_ENTITY_TO_MODEL.apply(savedPaymentMethod);
    }

    /**
     * Busca métodos de pago activos por usuario.
     */
    public List<PaymentMethodModel> findByUserList(Long userId) {
        log.debug("Buscando métodos de pago para usuario: {}", userId);
        return paymentMethodFacade.findByUserIdAndActive(userId)
                .stream()
                .map(PaymentMethodModel.FN_ENTITY_TO_MODEL)
                .collect(Collectors.toList());
    }

    /**
     * Busca métodos de pago activos por tenant con paginación.
     */
    public Page<PaymentMethodModel> findByTenant(Long tenantId, Pageable pageable) {
        log.debug("Buscando métodos de pago para tenant: {} con paginación", tenantId);
        return paymentMethodFacade.findByTenantAndActive(tenantId, pageable)
                .map(PaymentMethodModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca todos los métodos de pago con paginación.
     */
    public Page<PaymentMethodModel> findAll(Pageable pageable) {
        log.debug("Buscando todos los métodos de pago con paginación");
        return paymentMethodFacade.findAll(pageable)
                .map(PaymentMethodModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Busca un método de pago por ID.
     */
    public Optional<PaymentMethodModel> findById(Long id) {
        log.debug("Buscando método de pago por ID: {}", id);
        return paymentMethodFacade.findById(id)
                .map(PaymentMethodModel.FN_ENTITY_TO_MODEL);
    }

    /**
     * Actualiza un método de pago.
     */
    @Transactional
    public PaymentMethodModel updatePaymentMethod(Long paymentMethodId, PaymentMethodCreateModel updateModel) {
        log.info("Actualizando método de pago: {}", paymentMethodId);

        PaymentMethod paymentMethod = paymentMethodFacade.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado: " + paymentMethodId));

        // Validar días de corte y pago si es tarjeta de crédito
        if ("CREDIT".equals(updateModel.getAccountType())) {
            validateCreditCardDays(updateModel.getCutDay(), updateModel.getPaymentDay());
        }

        paymentMethod.setBankName(updateModel.getBankName());
        paymentMethod.setAlias(updateModel.getAlias());
        paymentMethod.setAccountType(updateModel.getAccountType());
        paymentMethod.setCutDay(updateModel.getCutDay());
        paymentMethod.setPaymentDay(updateModel.getPaymentDay());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod updatedPaymentMethod = paymentMethodFacade.save(paymentMethod);
        return PaymentMethodModel.FN_ENTITY_TO_MODEL.apply(updatedPaymentMethod);
    }

    /**
     * Elimina (desactiva) un método de pago.
     */
    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) {
        log.info("Eliminando método de pago: {}", paymentMethodId);

        PaymentMethod paymentMethod = paymentMethodFacade.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no encontrado: " + paymentMethodId));

        paymentMethod.setIsActive(false);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        paymentMethodFacade.save(paymentMethod);
    }

    // Métodos privados de validación

    private void validateCreditCardDays(PaymentMethodCreateModel createModel) {
        if ("CREDIT".equals(createModel.getAccountType())) {
            validateCreditCardDays(createModel.getCutDay(), createModel.getPaymentDay());
        }
    }

    private void validateCreditCardDays(Integer cutDay, Integer paymentDay) {
        if (cutDay != null && (cutDay < 1 || cutDay > 31)) {
            throw new IllegalArgumentException("El día de corte debe estar entre 1 y 31");
        }
        if (paymentDay != null && (paymentDay < 1 || paymentDay > 31)) {
            throw new IllegalArgumentException("El día de pago debe estar entre 1 y 31");
        }
    }
}
