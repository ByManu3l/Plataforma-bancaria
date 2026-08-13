package com.bancaria.prueba.cliente.dto;

import com.bancaria.prueba.cliente.TipoIdentificacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        TipoIdentificacion tipoIdentificacion,
        String numeroIdentificacion,
        String nombres,
        String apellido,
        String correoElectronico,
        LocalDate fechaNacimiento,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion
) {
}
