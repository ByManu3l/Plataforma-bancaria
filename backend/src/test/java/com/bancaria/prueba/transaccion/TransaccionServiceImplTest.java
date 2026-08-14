package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.CuentaMapper;
import com.bancaria.prueba.cuenta.CuentaRepository;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import com.bancaria.prueba.transaccion.dto.ConsignacionRequest;
import com.bancaria.prueba.transaccion.dto.EstadoCuentaResponse;
import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import com.bancaria.prueba.transaccion.dto.RetiroRequest;
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;
import com.bancaria.prueba.transaccion.operacion.CreditoOperacion;
import com.bancaria.prueba.transaccion.operacion.DebitoOperacion;
import com.bancaria.prueba.transaccion.operacion.OperacionCuentaRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private MovimientoMapper movimientoMapper;
    @Mock
    private CuentaMapper cuentaMapper;

    private TransaccionServiceImpl transaccionService;

    private UUID cuentaId;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        cuentaId = UUID.randomUUID();
        cuenta = cuenta(cuentaId, TipoCuenta.AHORROS, "5300000001", "100.00", true);

        // Se usan las estrategias reales: el objetivo es verificar el efecto sobre
        // el saldo, no aislar el servicio de su propia lógica de dominio.
        OperacionCuentaRegistry registry =
                new OperacionCuentaRegistry(List.of(new CreditoOperacion(), new DebitoOperacion()));
        transaccionService = new TransaccionServiceImpl(cuentaRepository, movimientoRepository, registry,
                new GmfCalculator(new BigDecimal("0.004")), movimientoMapper, cuentaMapper);
    }

    @Test
    void consignar_deberiaAcreditarElSaldoYRegistrarElMovimiento() {
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        transaccionService.consignar(request);

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("150.00");
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoMovimiento()).isEqualTo(TipoMovimiento.CONSIGNACION);
        assertThat(captor.getValue().getSaldoDisponible()).isEqualByComparingTo("150.00");
    }

    @Test
    void consignar_deberiaLanzarExcepcion_cuandoCuentaNoExiste() {
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.consignar(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void consignar_deberiaLanzarExcepcion_cuandoCuentaNoEstaActiva() {
        cuenta.setEstado(EstadoCuenta.INACTIVA);
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> transaccionService.consignar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está activa");
    }

    @Test
    void retirar_deberiaDebitarSinGmf_cuandoLaCuentaEstaExenta() {
        RetiroRequest request = new RetiroRequest(cuentaId, new BigDecimal("40.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        transaccionService.retirar(request);

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("60.00");
        verify(movimientoRepository, times(1)).save(any(Movimiento.class));
    }

    @Test
    void retirar_deberiaCobrarGmf_cuandoLaCuentaNoEstaExenta() {
        Cuenta noExenta = cuenta(cuentaId, TipoCuenta.AHORROS, "5300000002", "1000.00", false);
        RetiroRequest request = new RetiroRequest(cuentaId, new BigDecimal("100.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(noExenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        transaccionService.retirar(request);

        // 1000 - 100 (retiro) - 0.40 (4x1000 sobre 100) = 899.60
        assertThat(noExenta.getSaldo()).isEqualByComparingTo("899.60");

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Movimiento::getTipoMovimiento)
                .containsExactly(TipoMovimiento.RETIRO, TipoMovimiento.GMF);
        assertThat(captor.getAllValues().get(1).getValor()).isEqualByComparingTo("0.40");
    }

    @Test
    void retirar_deberiaFallar_cuandoElSaldoNoAlcanzaParaCubrirElGmf() {
        Cuenta noExenta = cuenta(cuentaId, TipoCuenta.AHORROS, "5300000002", "100.00", false);
        RetiroRequest request = new RetiroRequest(cuentaId, new BigDecimal("100.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(noExenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> transaccionService.retirar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("saldo negativo");
    }

    @Test
    void retirar_deberiaFallar_cuandoElSaldoEsInsuficienteEnCuentaDeAhorros() {
        RetiroRequest request = new RetiroRequest(cuentaId, new BigDecimal("500.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> transaccionService.retirar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saldo insuficiente");

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("100.00");
    }

    @Test
    void transferir_deberiaLanzarExcepcion_cuandoOrigenYDestinoSonLaMismaCuenta() {
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, cuentaId, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transaccionService.transferir(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no pueden ser la misma");

        verify(cuentaRepository, never()).findById(any());
    }

    @Test
    void transferir_deberiaDebitarOrigenYAcreditarDestino() {
        UUID destinoId = UUID.randomUUID();
        Cuenta destino = cuenta(destinoId, TipoCuenta.CORRIENTE, "3300000001", "20.00", false);
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, destinoId, new BigDecimal("30.00"));

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        transaccionService.transferir(request);

        // El origen está exento de GMF, por lo que solo se debita el valor transferido.
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("50.00");
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));
    }

    @Test
    void transferir_deberiaCobrarGmfSoloALaCuentaOrigen() {
        UUID destinoId = UUID.randomUUID();
        Cuenta origen = cuenta(cuentaId, TipoCuenta.CORRIENTE, "3300000009", "1000.00", false);
        Cuenta destino = cuenta(destinoId, TipoCuenta.CORRIENTE, "3300000001", "0.00", false);
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, destinoId, new BigDecimal("200.00"));

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(origen));
        when(cuentaRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        transaccionService.transferir(request);

        // 1000 - 200 - 0.80 (4x1000 sobre 200) = 799.20; el destino recibe el valor íntegro.
        assertThat(origen.getSaldo()).isEqualByComparingTo("799.20");
        assertThat(destino.getSaldo()).isEqualByComparingTo("200.00");
        verify(movimientoRepository, times(3)).save(any(Movimiento.class));
    }

    @Test
    void obtenerEstadoCuenta_deberiaRetornarCuentaYSusMovimientos() {
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)).thenReturn(List.of());
        when(cuentaMapper.toResponse(cuenta)).thenReturn(respuestaDe(cuenta));

        EstadoCuentaResponse response = transaccionService.obtenerEstadoCuenta(cuentaId);

        assertThat(response.cuenta().id()).isEqualTo(cuentaId);
        assertThat(response.movimientos()).isEmpty();
    }

    @Test
    void obtenerEstadoCuenta_deberiaLanzarResourceNotFound_cuandoCuentaNoExiste() {
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.obtenerEstadoCuenta(cuentaId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarMovimientos_sinFiltro_deberiaConsultarTodosLosMovimientos() {
        when(movimientoRepository.findAllByOrderByFechaDesc()).thenReturn(List.of());

        List<MovimientoResponse> movimientos = transaccionService.listarMovimientos(null);

        assertThat(movimientos).isEmpty();
        verify(movimientoRepository).findAllByOrderByFechaDesc();
    }

    @Test
    void listarMovimientos_conFiltro_deberiaConsultarSoloLaCuentaIndicada() {
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)).thenReturn(List.of());

        transaccionService.listarMovimientos(cuentaId);

        verify(movimientoRepository).findByCuentaIdOrderByFechaDesc(cuentaId);
        verify(movimientoRepository, never()).findAllByOrderByFechaDesc();
    }

    private Cuenta cuenta(UUID id, TipoCuenta tipo, String numero, String saldo, boolean exenta) {
        return Cuenta.builder()
                .id(id)
                .tipoCuenta(tipo)
                .numeroCuenta(numero)
                .estado(EstadoCuenta.ACTIVA)
                .saldo(new BigDecimal(saldo))
                .exentaGmf(exenta)
                .build();
    }

    private CuentaResponse respuestaDe(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.getTipoCuenta(), cuenta.getNumeroCuenta(), cuenta.getEstado(),
                cuenta.getSaldo(), cuenta.getSaldo(), cuenta.isExentaGmf(), null, null, UUID.randomUUID(), "Laura Gómez");
    }
}
