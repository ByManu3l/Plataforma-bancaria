-- Datos de ejemplo (DML) para poder probar la API/el frontend inmediatamente
-- después de levantar el proyecto. Todos los valores respetan las reglas de
-- negocio (clientes mayores de edad, formato de correo válido, etc.).

INSERT INTO clientes (id, tipo_identificacion, numero_identificacion, nombres, apellido, correo_electronico, fecha_nacimiento, fecha_creacion)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'CC', '1001234567', 'Laura', 'Gómez', 'laura.gomez@example.com', '1990-05-12', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'CC', '1009876543', 'Andrés', 'Ramírez', 'andres.ramirez@example.com', '1985-11-23', CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'CE', '900123456', 'María', 'López', 'maria.lopez@example.com', '1998-02-01', CURRENT_TIMESTAMP);

INSERT INTO cuentas (id, tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf, fecha_creacion, cliente_id)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'AHORROS', '5300000001', 'ACTIVA', 1500000.00, TRUE, CURRENT_TIMESTAMP, '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'CORRIENTE', '3300000001', 'ACTIVA', 500000.00, FALSE, CURRENT_TIMESTAMP, '22222222-2222-2222-2222-222222222222'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'AHORROS', '5300000002', 'ACTIVA', 0.00, TRUE, CURRENT_TIMESTAMP, '33333333-3333-3333-3333-333333333333');

INSERT INTO movimientos (id, cuenta_id, tipo_movimiento, valor, saldo_disponible, fecha, descripcion)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CONSIGNACION', 1500000.00, 1500000.00, CURRENT_TIMESTAMP, 'Consignación inicial'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'CONSIGNACION', 500000.00, 500000.00, CURRENT_TIMESTAMP, 'Consignación inicial');
