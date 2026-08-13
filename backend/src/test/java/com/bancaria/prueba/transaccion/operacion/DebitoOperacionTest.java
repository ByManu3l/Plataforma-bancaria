package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebitoOperacionTest {

    private final DebitoOperacion debitoOperacion = new DebitoOperacion();

    @Test
    void aplicar_deberiaDisminuirElSaldo() {
        Cuenta cuenta = cuenta(TipoCuenta.CORRIENTE, "100.00");

        debitoOperacion.aplicar(cuenta, new BigDecimal("40.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("60.00");
    }

    @Test
    void aplicar_deberiaLanzarExcepcion_cuandoCuentaAhorrosQuedariaEnNegativo() {
        Cuenta cuenta = cuenta(TipoCuenta.AHORROS, "30.00");

        assertThatThrownBy(() -> debitoOperacion.aplicar(cuenta, new BigDecimal("50.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("saldo negativo");

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("30.00");
    }

    @Test
    void aplicar_deberiaPermitirSobregiro_enCuentaCorriente() {
        Cuenta cuenta = cuenta(TipoCuenta.CORRIENTE, "30.00");

        debitoOperacion.aplicar(cuenta, new BigDecimal("50.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("-20.00");
    }

    private Cuenta cuenta(TipoCuenta tipoCuenta, String saldo) {
        return Cuenta.builder()
                .tipoCuenta(tipoCuenta)
                .numeroCuenta(tipoCuenta == TipoCuenta.AHORROS ? "5300000001" : "3300000001")
                .estado(EstadoCuenta.ACTIVA)
                .saldo(new BigDecimal(saldo))
                .build();
    }
}
