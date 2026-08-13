package com.bancaria.prueba.transaccion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaRequest(
        @NotNull(message = "La cuenta origen es obligatoria")
        UUID cuentaOrigenId,

        @NotNull(message = "La cuenta destino es obligatoria")
        UUID cuentaDestinoId,

        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor a transferir debe ser mayor a cero")
        BigDecimal valor
) {
}
