package com.bancaria.prueba.cliente;

import com.bancaria.prueba.cliente.dto.ClienteRequest;
import com.bancaria.prueba.cliente.dto.ClienteResponse;
import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRequest request;
    private Cliente cliente;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        request = new ClienteRequest(
                TipoIdentificacion.CC,
                "1001234567",
                "Laura",
                "Gómez",
                "laura.gomez@example.com",
                LocalDate.of(1990, 5, 12));
        cliente = Cliente.builder()
                .id(clienteId)
                .tipoIdentificacion(request.tipoIdentificacion())
                .numeroIdentificacion(request.numeroIdentificacion())
                .nombres(request.nombres())
                .apellido(request.apellido())
                .correoElectronico(request.correoElectronico())
                .fechaNacimiento(request.fechaNacimiento())
                .build();
    }

    @Test
    void crear_deberiaGuardarCliente_cuandoNoHayDuplicados() {
        when(clienteRepository.existsByNumeroIdentificacion(request.numeroIdentificacion())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(request.correoElectronico())).thenReturn(false);
        when(clienteMapper.toEntity(request)).thenReturn(cliente);
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(respuestaDe(cliente));

        ClienteResponse response = clienteService.crear(request);

        assertThat(response.id()).isEqualTo(clienteId);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void crear_deberiaLanzarExcepcion_cuandoNumeroIdentificacionYaExiste() {
        when(clienteRepository.existsByNumeroIdentificacion(request.numeroIdentificacion())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(request.numeroIdentificacion());

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crear_deberiaLanzarExcepcion_cuandoCorreoYaExiste() {
        when(clienteRepository.existsByNumeroIdentificacion(request.numeroIdentificacion())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(request.correoElectronico())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(request.correoElectronico());

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizar_deberiaActualizarCliente_cuandoExiste() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(clienteRepository.saveAndFlush(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenAnswer(invocation -> respuestaDe(invocation.getArgument(0)));

        ClienteResponse response = clienteService.actualizar(clienteId, request);

        assertThat(response.id()).isEqualTo(clienteId);
        verify(clienteMapper).actualizarEntidad(request, cliente);
        // saveAndFlush (y no save) es lo que garantiza que @PreUpdate se ejecute
        // antes de mapear la respuesta, para que viaje la fecha de modificación.
        verify(clienteRepository).saveAndFlush(cliente);
    }

    @Test
    void actualizar_deberiaLanzarResourceNotFound_cuandoClienteNoExiste() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(clienteId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_deberiaEliminarCliente_cuandoNoTieneCuentasVinculadas() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(cuentaRepository.existsByClienteId(clienteId)).thenReturn(false);

        clienteService.eliminar(clienteId);

        verify(clienteRepository, times(1)).delete(cliente);
    }

    @Test
    void eliminar_deberiaLanzarExcepcion_cuandoTieneCuentasVinculadas() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(cuentaRepository.existsByClienteId(clienteId)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.eliminar(clienteId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("productos financieros vinculados");

        verify(clienteRepository, never()).delete(any());
    }

    @Test
    void obtener_deberiaLanzarResourceNotFound_cuandoClienteNoExiste() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.obtener(clienteId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_deberiaRetornarTodosLosClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(respuestaDe(cliente));

        List<ClienteResponse> respuesta = clienteService.listar();

        assertThat(respuesta).hasSize(1);
    }

    @Test
    void actualizar_deberiaLanzarExcepcion_cuandoElNuevoNumeroDeIdentificacionYaEsDeOtroCliente() {
        ClienteRequest otro = new ClienteRequest(TipoIdentificacion.CC, "9999999999", "Ana", "Ruiz",
                cliente.getCorreoElectronico(), request.fechaNacimiento());
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByNumeroIdentificacion("9999999999")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.actualizar(clienteId, otro))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("9999999999");

        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizar_deberiaLanzarExcepcion_cuandoElNuevoCorreoYaEsDeOtroCliente() {
        ClienteRequest otro = new ClienteRequest(TipoIdentificacion.CC, cliente.getNumeroIdentificacion(),
                "Ana", "Ruiz", "ocupado@example.com", request.fechaNacimiento());
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCorreoElectronico("ocupado@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.actualizar(clienteId, otro))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ocupado@example.com");

        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void obtener_deberiaDevolverElCliente_cuandoExiste() {
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(respuestaDe(cliente));

        assertThat(clienteService.obtener(clienteId).id()).isEqualTo(clienteId);
    }

    private ClienteResponse respuestaDe(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getTipoIdentificacion(),
                cliente.getNumeroIdentificacion(),
                cliente.getNombres(),
                cliente.getApellido(),
                cliente.getCorreoElectronico(),
                cliente.getFechaNacimiento(),
                cliente.getFechaCreacion(),
                cliente.getFechaModificacion());
    }
}
