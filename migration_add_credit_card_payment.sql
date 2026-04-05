-- Migración: tabla credit_card_payment
-- Registra cómo se paga cada tarjeta de crédito en un periodo dado.
-- Un pago de tarjeta puede cubrirse con múltiples métodos de pago (débito, efectivo, etc.)
-- y cada línea genera automáticamente una Transaction de tipo CREDIT_PAYMENT.

CREATE TABLE IF NOT EXISTS credit_card_payment (
    id_credit_card_payment  BIGSERIAL PRIMARY KEY,
    id_tenant               BIGINT        NOT NULL,
    id_credit_card          BIGINT        NOT NULL,   -- PaymentMethod (CREDIT) que se está pagando
    billing_period_id       VARCHAR(50)   NOT NULL,   -- ej: "2026-03-02_2026-04-01"
    id_source_payment_method BIGINT       NOT NULL,   -- con qué método se paga (débito, efectivo, etc.)
    id_transaction          BIGINT,                   -- FK a la Transaction CREDIT_PAYMENT generada
    id_paid_by_user         BIGINT        NOT NULL,   -- quién registró/realizó este pago
    amount                  DECIMAL(15,2) NOT NULL,   -- cuánto se pagó con este método
    notes                   VARCHAR(255),
    paid_at                 TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ccp_credit_card   FOREIGN KEY (id_credit_card)           REFERENCES payment_method(id_payment_method),
    CONSTRAINT fk_ccp_source_pm     FOREIGN KEY (id_source_payment_method) REFERENCES payment_method(id_payment_method),
    CONSTRAINT fk_ccp_transaction   FOREIGN KEY (id_transaction)            REFERENCES transaction(id_transaction),
    CONSTRAINT fk_ccp_user          FOREIGN KEY (id_paid_by_user)           REFERENCES usuario(id_user)
);

CREATE INDEX IF NOT EXISTS idx_ccp_tenant          ON credit_card_payment (id_tenant);
CREATE INDEX IF NOT EXISTS idx_ccp_credit_card     ON credit_card_payment (id_credit_card);
CREATE INDEX IF NOT EXISTS idx_ccp_period          ON credit_card_payment (id_credit_card, billing_period_id);
CREATE INDEX IF NOT EXISTS idx_ccp_source_pm       ON credit_card_payment (id_source_payment_method);

