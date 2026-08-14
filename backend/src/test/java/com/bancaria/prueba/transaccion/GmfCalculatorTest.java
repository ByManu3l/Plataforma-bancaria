package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.cuenta.Cuenta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GmfCalculatorTest {

    private final GmfCalculator calculator = new GmfCalculator(new BigDecimal("0.004"));

    @Test
    void calcular_deberiaSerCero_cuandoLaCuentaEstaExenta() {
        Cuenta exenta = Cuenta.builder().exentaGmf(true).build();

        assertThat(calculator.calcular(exenta, new BigDecimal("1000000"))).isEqualByComparingTo("0");
    }

    @Test
    void calcular_deberiaAplicarElCuatroPorMil_cuandoLaCuentaNoEstaExenta() {
        Cuenta noExenta = Cuenta.builder().exentaGmf(false).build();

        assertThat(calculator.calcular(noExenta, new BigDecimal("1000000"))).isEqualByComparingTo("4000.00");
    }

    @Test
    void calcular_deberiaRedondearADosDecimales() {
        Cuenta noExenta = Cuenta.builder().exentaGmf(false).build();

        // 333.33 * 0.004 = 1.33332 -> 1.33
        assertThat(calculator.calcular(noExenta, new BigDecimal("333.33"))).isEqualByComparingTo("1.33");
    }
}
