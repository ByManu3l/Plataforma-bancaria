package com.bancaria.prueba.transaccion.operacion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperacionCuentaRegistryTest {

    @Test
    void para_deberiaResolverLaEstrategiaSegunElTipoDeOperacion() {
        OperacionCuentaRegistry registry =
                new OperacionCuentaRegistry(List.of(new CreditoOperacion(), new DebitoOperacion()));

        assertThat(registry.para(TipoOperacion.CREDITO)).isInstanceOf(CreditoOperacion.class);
        assertThat(registry.para(TipoOperacion.DEBITO)).isInstanceOf(DebitoOperacion.class);
    }

    @Test
    void para_deberiaFallar_cuandoNoHayEstrategiaRegistrada() {
        OperacionCuentaRegistry registry = new OperacionCuentaRegistry(List.of(new CreditoOperacion()));

        assertThatThrownBy(() -> registry.para(TipoOperacion.DEBITO))
                .isInstanceOf(IllegalStateException.class);
    }
}
