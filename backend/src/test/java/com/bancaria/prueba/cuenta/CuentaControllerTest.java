package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.dto.CambiarEstadoCuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuentaController.class)
class CuentaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CuentaService cuentaService;

    private final UUID cuentaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    @Test
    void crear_deberiaRetornar201_cuandoDatosValidos() throws Exception {
        CuentaRequest request = new CuentaRequest(TipoCuenta.AHORROS, clienteId, true);
        CuentaResponse response = respuesta(EstadoCuenta.ACTIVA, BigDecimal.ZERO);
        when(cuentaService.crear(any(CuentaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta").value("5300000001"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void crear_deberiaRetornar400_cuandoFaltaElTipoDeCuenta() throws Exception {
        String cuerpoInvalido = """
                {
                  "clienteId": "%s",
                  "exentaGmf": false
                }
                """.formatted(clienteId);

        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_deberiaRetornar200_cuandoElCambioEsValido() throws Exception {
        CambiarEstadoCuentaRequest request = new CambiarEstadoCuentaRequest(EstadoCuenta.INACTIVA);
        when(cuentaService.cambiarEstado(eq(cuentaId), eq(EstadoCuenta.INACTIVA)))
                .thenReturn(respuesta(EstadoCuenta.INACTIVA, BigDecimal.ZERO));

        mockMvc.perform(patch("/api/cuentas/{id}/estado", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));
    }

    @Test
    void cambiarEstado_deberiaRetornar422_cuandoNoSePuedeCancelarConSaldo() throws Exception {
        CambiarEstadoCuentaRequest request = new CambiarEstadoCuentaRequest(EstadoCuenta.CANCELADA);
        when(cuentaService.cambiarEstado(eq(cuentaId), eq(EstadoCuenta.CANCELADA)))
                .thenThrow(new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0"));

        mockMvc.perform(patch("/api/cuentas/{id}/estado", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void obtener_deberiaRetornar404_cuandoLaCuentaNoExiste() throws Exception {
        when(cuentaService.obtener(eq(cuentaId)))
                .thenThrow(new ResourceNotFoundException("No se encontró la cuenta con id " + cuentaId));

        mockMvc.perform(get("/api/cuentas/{id}", cuentaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_conClienteId_deberiaFiltrarPorCliente() throws Exception {
        when(cuentaService.listarPorCliente(clienteId)).thenReturn(List.of(respuesta(EstadoCuenta.ACTIVA, BigDecimal.ZERO)));

        mockMvc.perform(get("/api/cuentas").param("clienteId", clienteId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    private CuentaResponse respuesta(EstadoCuenta estado, BigDecimal saldo) {
        return new CuentaResponse(cuentaId, TipoCuenta.AHORROS, "5300000001", estado, saldo, true,
                null, null, clienteId, "Laura Gómez");
    }
}
