package com.bancaria.prueba.cuenta.dto;

import com.bancaria.prueba.cuenta.EstadoCuenta;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoCuentaRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoCuenta estado
) {
}
