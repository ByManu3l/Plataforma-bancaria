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
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;
import com.bancaria.prueba.transaccion.operacion.CreditoOperacion;
import com.bancaria.prueba.transaccion.operacion.DebitoOperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private CreditoOperacion creditoOperacion;
    @Mock
    private DebitoOperacion debitoOperacion;
    @Mock
    private MovimientoMapper movimientoMapper;
    @Mock
    private CuentaMapper cuentaMapper;

    @InjectMocks
    private TransaccionServiceImpl transaccionService;

    private UUID cuentaId;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        cuentaId = UUID.randomUUID();
        cuenta = Cuenta.builder()
                .id(cuentaId)
                .tipoCuenta(TipoCuenta.AHORROS)
                .numeroCuenta("5300000001")
                .estado(EstadoCuenta.ACTIVA)
                .saldo(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void consignar_deberiaAplicarCreditoYRegistrarMovimiento() {
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoMapper.toResponse(any(Movimiento.class))).thenReturn(
                new MovimientoResponse(UUID.randomUUID(), TipoMovimiento.CONSIGNACION, request.valor(),
                        cuenta.getSaldo(), null, cuentaId, cuenta.getNumeroCuenta(), null, null, null));

        MovimientoResponse response = transaccionService.consignar(request);

        verify(creditoOperacion).aplicar(cuenta, request.valor());
        verify(cuentaRepository).save(cuenta);
        assertThat(response.tipoMovimiento()).isEqualTo(TipoMovimiento.CONSIGNACION);
    }

    @Test
    void consignar_deberiaLanzarExcepcion_cuandoCuentaNoExiste() {
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.consignar(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(creditoOperacion, never()).aplicar(any(), any());
    }

    @Test
    void consignar_deberiaLanzarExcepcion_cuandoCuentaNoEstaActiva() {
        cuenta.setEstado(EstadoCuenta.INACTIVA);
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> transaccionService.consignar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está activa");

        verify(creditoOperacion, never()).aplicar(any(), any());
    }

    @Test
    void transferir_deberiaLanzarExcepcion_cuandoCuentaOrigenEsIgualADestino() {
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, cuentaId, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transaccionService.transferir(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no pueden ser la misma");

        verify(cuentaRepository, never()).findById(any());
    }

    @Test
    void transferir_deberiaAplicarDebitoEnOrigenYCreditoEnDestino() {
        UUID destinoId = UUID.randomUUID();
        Cuenta destino = Cuenta.builder()
                .id(destinoId)
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .numeroCuenta("3300000001")
                .estado(EstadoCuenta.ACTIVA)
                .saldo(new BigDecimal("20.00"))
                .build();
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, destinoId, new BigDecimal("30.00"));

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoMapper.toResponse(any(Movimiento.class))).thenReturn(
                new MovimientoResponse(UUID.randomUUID(), TipoMovimiento.TRANSFERENCIA_ENVIADA, request.valor(),
                        cuenta.getSaldo(), null, cuentaId, cuenta.getNumeroCuenta(), destinoId, destino.getNumeroCuenta(), null));

        MovimientoResponse response = transaccionService.transferir(request);

        verify(debitoOperacion).aplicar(cuenta, request.valor());
        verify(creditoOperacion).aplicar(destino, request.valor());
        verify(movimientoRepository, times(2)).save(any(Movimiento.class));
        assertThat(response.tipoMovimiento()).isEqualTo(TipoMovimiento.TRANSFERENCIA_ENVIADA);
    }

    @Test
    void obtenerEstadoCuenta_deberiaRetornarCuentaYSusMovimientos() {
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)).thenReturn(List.of());
        when(cuentaMapper.toResponse(cuenta)).thenReturn(
                new CuentaResponse(cuentaId, TipoCuenta.AHORROS, cuenta.getNumeroCuenta(), EstadoCuenta.ACTIVA,
                        cuenta.getSaldo(), true, null, null, UUID.randomUUID(), "Laura Gómez"));

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
}
