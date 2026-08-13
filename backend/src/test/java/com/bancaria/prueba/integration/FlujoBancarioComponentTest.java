package com.bancaria.prueba.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de componentes: ejercita Controller + Service + Repository + base de
 * datos real (H2, con el esquema creado por las migraciones Flyway), a
 * diferencia de los tests unitarios que mockean las dependencias.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoBancarioComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flujoCompleto_crearClienteCuentaYRealizarTransacciones() throws Exception {
        String correo = "test.componente." + UUID.randomUUID() + "@example.com";
        Map<String, Object> clienteRequest = Map.of(
                "tipoIdentificacion", "CC",
                "numeroIdentificacion", numeroIdentificacionUnico(),
                "nombres", "Camila",
                "apellido", "Torres",
                "correoElectronico", correo,
                "fechaNacimiento", LocalDate.now().minusYears(25).toString());

        String clienteBody = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String clienteId = objectMapper.readTree(clienteBody).get("id").asText();

        Map<String, Object> cuentaOrigenRequest = Map.of(
                "tipoCuenta", "AHORROS", "clienteId", clienteId, "exentaGmf", true);
        String cuentaOrigenBody = mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuentaOrigenRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta", org.hamcrest.Matchers.startsWith("53")))
                .andExpect(jsonPath("$.saldo").value(0))
                .andReturn().getResponse().getContentAsString();
        String cuentaOrigenId = objectMapper.readTree(cuentaOrigenBody).get("id").asText();

        Map<String, Object> cuentaDestinoRequest = Map.of(
                "tipoCuenta", "CORRIENTE", "clienteId", clienteId, "exentaGmf", false);
        String cuentaDestinoBody = mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuentaDestinoRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cuentaDestinoId = objectMapper.readTree(cuentaDestinoBody).get("id").asText();

        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaOrigenId, "valor", 200000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(200000));

        mockMvc.perform(post("/api/transacciones/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaOrigenId, "valor", 50000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(150000));

        mockMvc.perform(post("/api/transacciones/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cuentaOrigenId", cuentaOrigenId, "cuentaDestinoId", cuentaDestinoId, "valor", 100000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(50000));

        mockMvc.perform(get("/api/transacciones/cuentas/{cuentaId}/estado", cuentaOrigenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.saldo").value(50000))
                .andExpect(jsonPath("$.movimientos", org.hamcrest.Matchers.hasSize(3)));

        mockMvc.perform(get("/api/transacciones/cuentas/{cuentaId}/estado", cuentaDestinoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.saldo").value(100000));

        // Un cliente con cuentas vinculadas no puede eliminarse.
        mockMvc.perform(delete("/api/clientes/{id}", clienteId))
                .andExpect(status().isUnprocessableEntity());

        // Solo se puede cancelar una cuenta si su saldo es exactamente $0.
        mockMvc.perform(patch("/api/cuentas/{id}/estado", cuentaOrigenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estado", "CANCELADA"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void crearCliente_deberiaRetornar400_cuandoEsMenorDeEdad() throws Exception {
        Map<String, Object> clienteRequest = Map.of(
                "tipoIdentificacion", "CC",
                "numeroIdentificacion", numeroIdentificacionUnico(),
                "nombres", "Joven",
                "apellido", "Pérez",
                "correoElectronico", "menor." + UUID.randomUUID() + "@example.com",
                "fechaNacimiento", LocalDate.now().minusYears(15).toString());

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest)))
                .andExpect(status().isBadRequest());
    }

    private String numeroIdentificacionUnico() {
        return "ID" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
