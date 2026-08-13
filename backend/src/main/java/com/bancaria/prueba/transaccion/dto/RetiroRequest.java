package com.bancaria.prueba.transaccion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RetiroRequest(
        @NotNull(message = "La cuenta es obligatoria")
        UUID cuentaId,

        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor a retirar debe ser mayor a cero")
        BigDecimal valor
) {
}
