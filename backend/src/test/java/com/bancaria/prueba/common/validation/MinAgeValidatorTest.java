package com.bancaria.prueba.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MinAgeValidatorTest {

    private static class Sujeto {
        @MinAge(18)
        LocalDate fechaNacimiento;
    }

    private MinAgeValidator validator;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        validator = new MinAgeValidator();
        Field field = Sujeto.class.getDeclaredField("fechaNacimiento");
        validator.initialize(field.getAnnotation(MinAge.class));
    }

    @Test
    void isValid_deberiaSerFalso_cuandoElClienteEsMenorDeEdad() {
        LocalDate hace17Anios = LocalDate.now().minusYears(17);

        assertThat(validator.isValid(hace17Anios, null)).isFalse();
    }

    @Test
    void isValid_deberiaSerVerdadero_cuandoElClienteEsMayorDeEdad() {
        LocalDate hace18Anios = LocalDate.now().minusYears(18);

        assertThat(validator.isValid(hace18Anios, null)).isTrue();
    }

    @Test
    void isValid_deberiaSerFalso_cuandoLaFechaEsFutura() {
        LocalDate manana = LocalDate.now().plusDays(1);

        assertThat(validator.isValid(manana, null)).isFalse();
    }

    @Test
    void isValid_deberiaSerVerdadero_cuandoLaFechaEsNula() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
