package com.bancaria.prueba.cuenta.dto;

import com.bancaria.prueba.cuenta.TipoCuenta;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CuentaRequest(

        @NotNull(message = "El tipo de cuenta es obligatorio")
        TipoCuenta tipoCuenta,

        @NotNull(message = "El cliente al que pertenece la cuenta es obligatorio")
        UUID clienteId,

        boolean exentaGmf
) {
}
