-- Control de concurrencia optimista sobre las cuentas: la columna `version` la
-- gestiona Hibernate (@Version) e impide la pérdida de actualizaciones cuando
-- dos transacciones modifican el saldo de la misma cuenta simultáneamente.
ALTER TABLE cuentas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Se habilita el movimiento de tipo GMF (gravamen a los movimientos financieros,
-- "4x1000"), que se registra como un movimiento débito propio para que quede
-- trazado de forma independiente en el estado de cuenta.
ALTER TABLE movimientos DROP CONSTRAINT ck_movimientos_tipo_movimiento;

ALTER TABLE movimientos ADD CONSTRAINT ck_movimientos_tipo_movimiento
    CHECK (tipo_movimiento IN ('CONSIGNACION', 'RETIRO', 'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA', 'GMF'));
