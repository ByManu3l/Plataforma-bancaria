package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.cliente.Cliente;
import com.bancaria.prueba.cliente.ClienteRepository;
import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.dto.CuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private NumeroCuentaGenerator numeroCuentaGenerator;
    @Mock
    private CuentaMapper cuentaMapper;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private UUID clienteId;
    private UUID cuentaId;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        cuentaId = UUID.randomUUID();
        cliente = Cliente.builder().id(clienteId).nombres("Laura").apellido("Gómez").build();
    }

    @Test
    void crear_deberiaCrearCuentaActivaConSaldoCero_cuandoClienteExiste() {
        CuentaRequest request = new CuentaRequest(TipoCuenta.AHORROS, clienteId, true);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(numeroCuentaGenerator.generar(TipoCuenta.AHORROS)).thenReturn("5300000001");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cuentaMapper.toResponse(any(Cuenta.class))).thenAnswer(invocation -> respuestaDe(invocation.getArgument(0)));

        CuentaResponse response = cuentaService.crear(request);

        assertThat(response.estado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(response.saldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.numeroCuenta()).isEqualTo("5300000001");
    }

    @Test
    void crear_deberiaLanzarResourceNotFound_cuandoClienteNoExiste() {
        CuentaRequest request = new CuentaRequest(TipoCuenta.CORRIENTE, clienteId, false);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_deberiaCancelar_cuandoSaldoEsCero() {
        Cuenta cuenta = cuentaConSaldo(BigDecimal.ZERO);
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(cuenta)).thenReturn(cuenta);
        when(cuentaMapper.toResponse(cuenta)).thenReturn(respuestaDe(cuenta));

        CuentaResponse response = cuentaService.cambiarEstado(cuentaId, EstadoCuenta.CANCELADA);

        assertThat(response.estado()).isEqualTo(EstadoCuenta.CANCELADA);
    }

    @Test
    void cambiarEstado_deberiaLanzarExcepcion_cuandoCancelarConSaldoDiferenteDeCero() {
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("1000.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaService.cambiarEstado(cuentaId, EstadoCuenta.CANCELADA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("saldo igual a $0");

        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_deberiaInactivar_sinImportarElSaldo() {
        Cuenta cuenta = cuentaConSaldo(new BigDecimal("1000.00"));
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(cuenta)).thenReturn(cuenta);
        when(cuentaMapper.toResponse(cuenta)).thenReturn(respuestaDe(cuenta));

        CuentaResponse response = cuentaService.cambiarEstado(cuentaId, EstadoCuenta.INACTIVA);

        assertThat(response.estado()).isEqualTo(EstadoCuenta.INACTIVA);
    }

    @Test
    void obtener_deberiaLanzarResourceNotFound_cuandoCuentaNoExiste() {
        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuentaService.obtener(cuentaId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorCliente_deberiaFiltrarPorClienteId() {
        Cuenta cuenta = cuentaConSaldo(BigDecimal.ZERO);
        when(cuentaRepository.findByClienteId(clienteId)).thenReturn(List.of(cuenta));
        when(cuentaMapper.toResponse(cuenta)).thenReturn(respuestaDe(cuenta));

        List<CuentaResponse> respuesta = cuentaService.listarPorCliente(clienteId);

        assertThat(respuesta).hasSize(1);
    }

    private Cuenta cuentaConSaldo(BigDecimal saldo) {
        return Cuenta.builder()
                .id(cuentaId)
                .tipoCuenta(TipoCuenta.AHORROS)
                .numeroCuenta("5300000001")
                .estado(EstadoCuenta.ACTIVA)
                .saldo(saldo)
                .exentaGmf(true)
                .cliente(cliente)
                .build();
    }

    private CuentaResponse respuestaDe(Cuenta cuenta) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getTipoCuenta(),
                cuenta.getNumeroCuenta(),
                cuenta.getEstado(),
                cuenta.getSaldo(),
                cuenta.isExentaGmf(),
                cuenta.getFechaCreacion(),
                cuenta.getFechaModificacion(),
                cuenta.getCliente().getId(),
                cuenta.getCliente().getNombres() + " " + cuenta.getCliente().getApellido());
    }
}
