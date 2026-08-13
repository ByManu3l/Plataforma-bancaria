package com.bancaria.prueba.cuenta.dto;

import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CuentaResponse(
        UUID id,
        TipoCuenta tipoCuenta,
        String numeroCuenta,
        EstadoCuenta estado,
        BigDecimal saldo,
        boolean exentaGmf,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion,
        UUID clienteId,
        String clienteNombreCompleto
) {
}
