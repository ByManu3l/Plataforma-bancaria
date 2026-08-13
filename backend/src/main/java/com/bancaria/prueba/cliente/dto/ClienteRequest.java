package com.bancaria.prueba.cliente.dto;

import com.bancaria.prueba.cliente.TipoIdentificacion;
import com.bancaria.prueba.common.validation.MinAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClienteRequest(

        @NotNull(message = "El tipo de identificación es obligatorio")
        TipoIdentificacion tipoIdentificacion,

        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(max = 20, message = "El número de identificación no puede superar 20 caracteres")
        String numeroIdentificacion,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener al menos 2 caracteres")
        String nombres,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener al menos 2 caracteres")
        String apellido,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Pattern(regexp = "^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", message = "El correo electrónico no tiene un formato válido")
        String correoElectronico,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        @MinAge(value = 18, message = "El cliente debe ser mayor de edad")
        LocalDate fechaNacimiento
) {
}
