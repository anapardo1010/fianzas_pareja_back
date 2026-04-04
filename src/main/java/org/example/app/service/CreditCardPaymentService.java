package org.example.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.app.domain.entity.*;
import org.example.app.facade.*;
import org.example.app.web.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardPaymentService {

    private final CreditCardPaymentFacade creditCardPaymentFacade;
    private final PaymentMethodFacade paymentMethodFacade;
    private final UserFacade userFacade;
    private final TransactionFacade transactionFacade;
    private final TenantFacade tenantFacade;

    // =========================================================================
    // Registrar pago
    // =========================================================================

    /**
     * Registra cómo se paga una tarjeta de crédito en un periodo dado.
     *
     * Por cada ítem en {@code request.payments}:
     *  1. Valida que el método de origen no sea CREDIT.
     *  2. Crea una Transaction de tipo CREDIT_PAYMENT en el método de origen.
     *  3. Persiste la fila en credit_card_payment enlazada a esa Transaction.
     *
     * No valida que la suma cubra exactamente el totalDue para permitir pagos parciales,
     * pero sí valida que cada monto sea positivo.
     */
    @Transactional
    public List<CreditCardPaymentItemResponse> registerPayment(Long tenantId, CreditCardPaymentRequest request) {
        log.info("Registrando pago de tarjeta {} periodo {} para tenant {}",
                request.getCreditCardId(), request.getPeriodId(), tenantId);

        // Validar tarjeta de crédito destino
        PaymentMethod creditCard = paymentMethodFacade.findById(request.getCreditCardId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tarjeta de crédito no encontrada: " + request.getCreditCardId()));

        if (!"CREDIT".equalsIgnoreCase(creditCard.getAccountType())) {
            throw new IllegalArgumentException(
                    "El método de pago " + request.getCreditCardId() + " no es una tarjeta de crédito");
        }

        Tenant tenant = tenantFacade.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado: " + tenantId));

        // Validar que la suma de pagos sea positiva
        if (request.getPayments() == null || request.getPayments().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un pago");
        }

        BigDecimal sumPayments = request.getPayments().stream()
                .map(CreditCardPaymentItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumPayments.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La suma de los pagos debe ser mayor a cero");
        }

        List<CreditCardPaymentItemResponse> responses = new ArrayList<>();

        for (CreditCardPaymentItemRequest item : request.getPayments()) {
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Cada monto de pago debe ser mayor a cero");
            }

            // Método de pago origen
            PaymentMethod sourcePm = paymentMethodFacade.findById(item.getSourcePaymentMethodId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Método de pago origen no encontrado: " + item.getSourcePaymentMethodId()));

            if ("CREDIT".equalsIgnoreCase(sourcePm.getAccountType())) {
                throw new IllegalArgumentException(
                        "No se puede usar una tarjeta de crédito como método de origen para pagar otra tarjeta");
            }

            // Usuario que paga
            User paidByUser = userFacade.findById(item.getPaidByUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Usuario no encontrado: " + item.getPaidByUserId()));

            // 1. Crear Transaction CREDIT_PAYMENT en el método origen
            String description = "Pago " + creditCard.getBankName()
                    + (creditCard.getAlias() != null ? " (" + creditCard.getAlias() + ")" : "")
                    + " — periodo " + request.getPeriodId();

            Transaction tx = new Transaction(
                    tenant,
                    paidByUser,
                    null,           // sin categoría (es un pago de tarjeta)
                    sourcePm,
                    description,
                    item.getAmount(),
                    LocalDate.now(),
                    false,          // no es compartido (es un pago real)
                    "CREDIT_PAYMENT",
                    false,
                    null
            );
            Transaction savedTx = transactionFacade.save(tx);
            log.info("Transaction CREDIT_PAYMENT creada id={} por ${} en método {}",
                    savedTx.getId(), item.getAmount(), sourcePm.getBankName());

            // 2. Guardar registro en credit_card_payment
            CreditCardPayment payment = new CreditCardPayment(
                    tenantId,
                    creditCard,
                    request.getPeriodId(),
                    sourcePm,
                    paidByUser,
                    item.getAmount(),
                    item.getNotes()
            );
            payment.setTransaction(savedTx);
            CreditCardPayment saved = creditCardPaymentFacade.save(payment);
            log.info("CreditCardPayment guardado id={}", saved.getId());

            responses.add(toResponse(saved));
        }

        log.info("Pago registrado: {} items para tarjeta {} periodo {}",
                responses.size(), request.getCreditCardId(), request.getPeriodId());
        return responses;
    }

    // =========================================================================
    // Historial
    // =========================================================================

    /**
     * Devuelve todos los pagos registrados de un tenant, ordenados por fecha descendente.
     */
    public List<CreditCardPaymentItemResponse> getHistoryByTenant(Long tenantId) {
        return creditCardPaymentFacade.findByTenant(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve los pagos de una tarjeta específica (todos los periodos).
     */
    public List<CreditCardPaymentItemResponse> getHistoryByCard(Long creditCardId) {
        return creditCardPaymentFacade.findByCard(creditCardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve los pagos de una tarjeta en un periodo específico.
     */
    public List<CreditCardPaymentItemResponse> getByCardAndPeriod(Long creditCardId, String periodId) {
        return creditCardPaymentFacade.findByCardAndPeriod(creditCardId, periodId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve cuánto se ha pagado ya de una tarjeta en un periodo.
     * Útil para que el front muestre el monto pendiente restante.
     */
    public BigDecimal getPaidAmountByCardAndPeriod(Long creditCardId, String periodId) {
        return creditCardPaymentFacade.sumPaidByCardAndPeriod(creditCardId, periodId);
    }

    // =========================================================================
    // Mapper
    // =========================================================================

    private CreditCardPaymentItemResponse toResponse(CreditCardPayment p) {
        PaymentMethod cc  = p.getCreditCard();
        PaymentMethod src = p.getSourcePaymentMethod();
        User user         = p.getPaidByUser();

        return new CreditCardPaymentItemResponse(
                p.getId(),
                cc  != null ? cc.getId()    : null,
                cc  != null ? cc.getAlias() : null,
                cc  != null ? cc.getBankName() : null,
                p.getBillingPeriodId(),
                src != null ? src.getId()    : null,
                src != null ? src.getAlias() : null,
                src != null ? src.getBankName() : null,
                p.getTransaction() != null ? p.getTransaction().getId() : null,
                user != null ? user.getId()   : null,
                user != null ? user.getName() : null,
                p.getAmount(),
                p.getNotes(),
                p.getPaidAt()
        );
    }
}

