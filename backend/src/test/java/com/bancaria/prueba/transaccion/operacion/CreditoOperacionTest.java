package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreditoOperacionTest {

    private final CreditoOperacion creditoOperacion = new CreditoOperacion();

    @Test
    void aplicar_deberiaIncrementarElSaldo() {
        Cuenta cuenta = Cuenta.builder()
                .tipoCuenta(TipoCuenta.AHORROS)
                .estado(EstadoCuenta.ACTIVA)
                .saldo(new BigDecimal("100.00"))
                .build();

        creditoOperacion.aplicar(cuenta, new BigDecimal("50.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
    }
}
