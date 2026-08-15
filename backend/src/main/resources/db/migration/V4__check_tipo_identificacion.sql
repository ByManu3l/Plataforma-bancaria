-- Coherencia del modelo: las columnas que representan un enum ya estaban
-- protegidas con CHECK en `cuentas` y `movimientos`, pero `clientes` había
-- quedado sin la restricción equivalente. Se añade para que la base rechace
-- por sí sola cualquier tipo de identificación que la aplicación no admita.
--
-- No se incluye 'TI' (tarjeta de identidad) porque es el documento de las
-- personas de 7 a 17 años y todo cliente debe ser mayor de edad.
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_tipo_identificacion
    CHECK (tipo_identificacion IN ('CC', 'CE', 'PASAPORTE'));
