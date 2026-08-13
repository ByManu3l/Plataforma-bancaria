-- DDL inicial: clientes, cuentas (productos financieros) y movimientos (transacciones).
-- Refleja exactamente el mapeo JPA de Cliente, Cuenta y Movimiento.

CREATE TABLE clientes (
    id                    UUID PRIMARY KEY,
    tipo_identificacion   VARCHAR(20)  NOT NULL,
    numero_identificacion VARCHAR(20)  NOT NULL,
    nombres               VARCHAR(100) NOT NULL,
    apellido              VARCHAR(100) NOT NULL,
    correo_electronico    VARCHAR(150) NOT NULL,
    fecha_nacimiento      DATE         NOT NULL,
    fecha_creacion        TIMESTAMP    NOT NULL,
    fecha_modificacion    TIMESTAMP,
    CONSTRAINT uq_clientes_numero_identificacion UNIQUE (numero_identificacion),
    CONSTRAINT uq_clientes_correo_electronico UNIQUE (correo_electronico)
);

CREATE TABLE cuentas (
    id                  UUID PRIMARY KEY,
    tipo_cuenta         VARCHAR(20)    NOT NULL,
    numero_cuenta       VARCHAR(10)    NOT NULL,
    estado              VARCHAR(20)    NOT NULL,
    saldo               NUMERIC(18, 2) NOT NULL,
    exenta_gmf          BOOLEAN        NOT NULL DEFAULT FALSE,
    fecha_creacion      TIMESTAMP      NOT NULL,
    fecha_modificacion  TIMESTAMP,
    cliente_id          UUID           NOT NULL,
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_cuentas_tipo_cuenta CHECK (tipo_cuenta IN ('AHORROS', 'CORRIENTE')),
    CONSTRAINT ck_cuentas_estado CHECK (estado IN ('ACTIVA', 'INACTIVA', 'CANCELADA')),
    CONSTRAINT ck_cuentas_saldo_ahorros_no_negativo
        CHECK (tipo_cuenta <> 'AHORROS' OR saldo >= 0)
);

CREATE INDEX ix_cuentas_cliente_id ON cuentas (cliente_id);

CREATE TABLE movimientos (
    id                     UUID           PRIMARY KEY,
    cuenta_id              UUID           NOT NULL,
    tipo_movimiento        VARCHAR(30)    NOT NULL,
    valor                  NUMERIC(18, 2) NOT NULL,
    saldo_disponible       NUMERIC(18, 2) NOT NULL,
    fecha                  TIMESTAMP      NOT NULL,
    cuenta_relacionada_id  UUID,
    descripcion            VARCHAR(255),
    CONSTRAINT fk_movimientos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_movimientos_cuenta_relacionada FOREIGN KEY (cuenta_relacionada_id) REFERENCES cuentas (id),
    CONSTRAINT ck_movimientos_tipo_movimiento
        CHECK (tipo_movimiento IN ('CONSIGNACION', 'RETIRO', 'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA'))
);

CREATE INDEX ix_movimientos_cuenta_id ON movimientos (cuenta_id);
CREATE INDEX ix_movimientos_cuenta_relacionada_id ON movimientos (cuenta_relacionada_id);
