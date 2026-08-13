package com.bancaria.prueba.cuenta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumeroCuentaGeneratorTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Test
    void generar_deberiaEmpezarConElPrefijoDeAhorrosYTenerDiezDigitos() {
        when(cuentaRepository.existsByNumeroCuenta(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        NumeroCuentaGenerator generator = new NumeroCuentaGenerator(cuentaRepository);

        String numero = generator.generar(TipoCuenta.AHORROS);

        assertThat(numero).hasSize(10).startsWith("53");
    }

    @Test
    void generar_deberiaEmpezarConElPrefijoDeCorrienteYTenerDiezDigitos() {
        when(cuentaRepository.existsByNumeroCuenta(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        NumeroCuentaGenerator generator = new NumeroCuentaGenerator(cuentaRepository);

        String numero = generator.generar(TipoCuenta.CORRIENTE);

        assertThat(numero).hasSize(10).startsWith("33");
    }

    @Test
    void generar_deberiaReintentar_cuandoElNumeroYaExiste() {
        when(cuentaRepository.existsByNumeroCuenta(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, true, false);
        NumeroCuentaGenerator generator = new NumeroCuentaGenerator(cuentaRepository);

        String numero = generator.generar(TipoCuenta.AHORROS);

        assertThat(numero).hasSize(10).startsWith("53");
    }
}
