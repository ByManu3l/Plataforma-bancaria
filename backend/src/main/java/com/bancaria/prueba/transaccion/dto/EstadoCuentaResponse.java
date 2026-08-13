package com.bancaria.prueba.transaccion.dto;

import com.bancaria.prueba.cuenta.dto.CuentaResponse;

import java.util.List;

public record EstadoCuentaResponse(
        CuentaResponse cuenta,
        List<MovimientoResponse> movimientos
) {
}
