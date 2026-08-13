package com.bancaria.prueba.transaccion.dto;

import com.bancaria.prueba.transaccion.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MovimientoResponse(
        UUID id,
        TipoMovimiento tipoMovimiento,
        BigDecimal valor,
        BigDecimal saldoDisponible,
        LocalDateTime fecha,
        UUID cuentaId,
        String numeroCuenta,
        UUID cuentaRelacionadaId,
        String numeroCuentaRelacionada,
        String descripcion
) {
}
