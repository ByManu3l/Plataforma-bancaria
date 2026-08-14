package com.bancaria.prueba.integration;

import com.bancaria.prueba.cliente.Cliente;
import com.bancaria.prueba.cliente.ClienteRepository;
import com.bancaria.prueba.cliente.TipoIdentificacion;
import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.CuentaRepository;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el control de concurrencia optimista sobre las cuentas.
 *
 * <p>Sin este control, dos operaciones que leen el mismo saldo y lo modifican
 * producirían una pérdida de actualización (lost update): la segunda en
 * confirmar sobrescribiría el resultado de la primera. El nivel de aislamiento
 * READ COMMITTED por sí solo no evita ese escenario, por eso la entidad Cuenta
 * lleva un campo @Version.
 *
 * <p>La clase no es transaccional a propósito: así cada llamada al repositorio
 * usa su propia transacción y contexto de persistencia, reproduciendo dos
 * operaciones que compiten por la misma fila.
 */
@SpringBootTest
class ConcurrenciaCuentaComponentTest {

    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void dosOperacionesSimultaneasSobreLaMismaCuenta_laSegundaDebeFallarEnLugarDeSobrescribir() {
        UUID cuentaId = crearCuentaConSaldo(new BigDecimal("1000.00"));

        Cuenta lecturaA = cuentaRepository.findById(cuentaId).orElseThrow();
        Cuenta lecturaB = cuentaRepository.findById(cuentaId).orElseThrow();
        assertThat(lecturaA.getVersion()).isEqualTo(lecturaB.getVersion());

        // La operación A confirma primero: descuenta 400 y sube la versión.
        lecturaA.setSaldo(lecturaA.getSaldo().subtract(new BigDecimal("400.00")));
        cuentaRepository.saveAndFlush(lecturaA);

        // La operación B trabaja sobre un estado ya obsoleto. Sin @Version dejaría
        // el saldo en 700 y el retiro de A se perdería; con @Version, falla.
        lecturaB.setSaldo(lecturaB.getSaldo().subtract(new BigDecimal("300.00")));
        assertThatThrownBy(() -> cuentaRepository.saveAndFlush(lecturaB))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // El saldo conserva el resultado de la operación que sí confirmó.
        assertThat(cuentaRepository.findById(cuentaId).orElseThrow().getSaldo())
                .isEqualByComparingTo("600.00");
    }

    @Test
    void laVersionDeberiaIncrementarseConCadaActualizacionConfirmada() {
        UUID cuentaId = crearCuentaConSaldo(new BigDecimal("100.00"));

        Long versionInicial = cuentaRepository.findById(cuentaId).orElseThrow().getVersion();

        Cuenta cuenta = cuentaRepository.findById(cuentaId).orElseThrow();
        cuenta.setSaldo(new BigDecimal("150.00"));
        cuentaRepository.saveAndFlush(cuenta);

        assertThat(cuentaRepository.findById(cuentaId).orElseThrow().getVersion())
                .isGreaterThan(versionInicial);
    }

    private UUID crearCuentaConSaldo(BigDecimal saldo) {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .tipoIdentificacion(TipoIdentificacion.CC)
                .numeroIdentificacion("CC" + sufijo)
                .nombres("Concurrencia")
                .apellido("Prueba")
                .correoElectronico("concurrencia." + sufijo + "@example.com")
                .fechaNacimiento(LocalDate.now().minusYears(30))
                .build());

        Cuenta cuenta = cuentaRepository.save(Cuenta.builder()
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .numeroCuenta("33" + sufijo)
                .estado(EstadoCuenta.ACTIVA)
                .saldo(saldo)
                .exentaGmf(true)
                .cliente(cliente)
                .build());
        return cuenta.getId();
    }
}
