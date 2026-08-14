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
        /**
         * Saldo del que el titular puede disponer. Coincide con el saldo contable
         * porque el alcance de la prueba no contempla retenciones ni embargos;
         * se expone como campo propio para dejar explícita la distinción.
         */
        BigDecimal saldoDisponible,
        boolean exentaGmf,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion,
        UUID clienteId,
        String clienteNombreCompleto
) {
}
