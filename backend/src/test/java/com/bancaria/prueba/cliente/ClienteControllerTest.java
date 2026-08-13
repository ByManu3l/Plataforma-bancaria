package com.bancaria.prueba.cliente;

import com.bancaria.prueba.cliente.dto.ClienteRequest;
import com.bancaria.prueba.cliente.dto.ClienteResponse;
import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ClienteService clienteService;

    private final UUID clienteId = UUID.randomUUID();

    @Test
    void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {
        ClienteRequest request = new ClienteRequest(TipoIdentificacion.CC, "1001234567", "Laura", "Gómez",
                "laura.gomez@example.com", LocalDate.of(1990, 5, 12));
        ClienteResponse response = new ClienteResponse(clienteId, request.tipoIdentificacion(), request.numeroIdentificacion(),
                request.nombres(), request.apellido(), request.correoElectronico(), request.fechaNacimiento(),
                LocalDateTime.now(), null);
        when(clienteService.crear(any(ClienteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(clienteId.toString()))
                .andExpect(jsonPath("$.numeroIdentificacion").value("1001234567"));
    }

    @Test
    void crear_deberiaRetornar400_cuandoFaltaElNombre() throws Exception {
        String cuerpoInvalido = """
                {
                  "tipoIdentificacion": "CC",
                  "numeroIdentificacion": "1001234567",
                  "apellido": "Gómez",
                  "correoElectronico": "laura.gomez@example.com",
                  "fechaNacimiento": "1990-05-12"
                }
                """;

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nombres"));
    }

    @Test
    void crear_deberiaRetornar422_cuandoLaReglaDeNegocioFalla() throws Exception {
        ClienteRequest request = new ClienteRequest(TipoIdentificacion.CC, "1001234567", "Laura", "Gómez",
                "laura.gomez@example.com", LocalDate.of(1990, 5, 12));
        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new BusinessRuleException("Ya existe un cliente registrado con el correo laura.gomez@example.com"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void obtener_deberiaRetornar404_cuandoElClienteNoExiste() throws Exception {
        when(clienteService.obtener(eq(clienteId)))
                .thenThrow(new ResourceNotFoundException("No se encontró el cliente con id " + clienteId));

        mockMvc.perform(get("/api/clientes/{id}", clienteId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_deberiaRetornar200_conLaListaDeClientes() throws Exception {
        ClienteResponse response = new ClienteResponse(clienteId, TipoIdentificacion.CC, "1001234567", "Laura",
                "Gómez", "laura.gomez@example.com", LocalDate.of(1990, 5, 12), LocalDateTime.now(), null);
        when(clienteService.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clienteId.toString()));
    }

    @Test
    void eliminar_deberiaRetornar204_cuandoSeEliminaCorrectamente() throws Exception {
        mockMvc.perform(delete("/api/clientes/{id}", clienteId))
                .andExpect(status().isNoContent());
    }
}
