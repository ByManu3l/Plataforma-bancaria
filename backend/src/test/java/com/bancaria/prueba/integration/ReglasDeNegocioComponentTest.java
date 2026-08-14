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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de componentes centrada en las reglas de negocio del enunciado y en el
 * contrato HTTP de error, ejercitando Controller + Service + Repository + base
 * de datos real (H2 con el esquema generado por Flyway).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReglasDeNegocioComponentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void actualizarCliente_deberiaDevolverLaFechaDeModificacionCalculada() throws Exception {
        String clienteId = crearCliente();

        String respuestaCreacion = mockMvc.perform(get("/api/clientes/{id}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaCreacion").exists())
                .andExpect(jsonPath("$.fechaModificacion").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(respuestaCreacion).get("fechaModificacion").isNull()).isTrue();

        Map<String, Object> actualizacion = Map.of(
                "tipoIdentificacion", "CC",
                "numeroIdentificacion", numeroIdentificacionUnico(),
                "nombres", "Camila Andrea",
                "apellido", "Torres",
                "correoElectronico", correoUnico(),
                "fechaNacimiento", LocalDate.now().minusYears(30).toString());

        // El PDF exige que la fecha de modificación se calcule automáticamente:
        // debe viajar ya resuelta en la respuesta del PUT, no solo quedar en la BD.
        mockMvc.perform(put("/api/clientes/{id}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizacion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Camila Andrea"))
                .andExpect(jsonPath("$.fechaModificacion").isNotEmpty());
    }

    @Test
    void cambiarEstadoDeCuenta_deberiaDevolverLaFechaDeModificacionYSerCancelacionDefinitiva() throws Exception {
        String cuentaId = crearCuenta(crearCliente(), "AHORROS", true);

        mockMvc.perform(patch("/api/cuentas/{id}/estado", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estado", "CANCELADA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaModificacion").isNotEmpty());

        // Una cuenta cancelada no se puede reactivar.
        mockMvc.perform(patch("/api/cuentas/{id}/estado", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("estado", "ACTIVA"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retiroSobreCuentaNoExenta_deberiaCobrarGmfComoMovimientoIndependiente() throws Exception {
        String cuentaId = crearCuenta(crearCliente(), "CORRIENTE", false);

        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaId, "valor", 1000000))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/transacciones/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaId, "valor", 100000))))
                .andExpect(status().isOk());

        // 1.000.000 - 100.000 - 400 (4x1000 sobre el retiro) = 899.600
        mockMvc.perform(get("/api/transacciones/cuentas/{cuentaId}/estado", cuentaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.saldo").value(899600.00))
                .andExpect(jsonPath("$.cuenta.saldoDisponible").value(899600.00))
                .andExpect(jsonPath("$.movimientos", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.movimientos[?(@.tipoMovimiento=='GMF')]",
                        org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.movimientos[?(@.tipoMovimiento=='GMF')].valor",
                        org.hamcrest.Matchers.contains(400.00)));
    }

    @Test
    void cuentaExentaDeGmf_noDeberiaGenerarMovimientoDeGravamen() throws Exception {
        String cuentaId = crearCuenta(crearCliente(), "AHORROS", true);

        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaId, "valor", 500000))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/transacciones/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaId, "valor", 200000))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transacciones/cuentas/{cuentaId}/estado", cuentaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.saldo").value(300000.00))
                .andExpect(jsonPath("$.movimientos", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void actualizarExencionDeGmfDeUnaCuenta_deberiaPersistirse() throws Exception {
        String cuentaId = crearCuenta(crearCliente(), "AHORROS", true);

        mockMvc.perform(patch("/api/cuentas/{id}", cuentaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exentaGmf", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exentaGmf").value(false));

        mockMvc.perform(get("/api/cuentas/{id}", cuentaId))
                .andExpect(jsonPath("$.exentaGmf").value(false));
    }

    @Test
    void listarMovimientos_deberiaPermitirFiltrarPorCuenta() throws Exception {
        String cuentaId = crearCuenta(crearCliente(), "AHORROS", true);
        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cuentaId", cuentaId, "valor", 1000))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transacciones").param("cuentaId", cuentaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(get("/api/transacciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void identificadorMalFormado_deberiaResponder400YNo500() throws Exception {
        mockMvc.perform(get("/api/clientes/{id}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void valorDeEnumNoAdmitido_deberiaResponder400YNo500() throws Exception {
        String cuerpo = """
                {
                  "tipoIdentificacion": "NO_EXISTE",
                  "numeroIdentificacion": "%s",
                  "nombres": "Ana",
                  "apellido": "Ruiz",
                  "correoElectronico": "%s",
                  "fechaNacimiento": "1990-01-01"
                }
                """.formatted(numeroIdentificacionUnico(), correoUnico());

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalFormado_deberiaResponder400YNo500() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaInexistente_deberiaResponder404YNo500() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound());
    }

    private String crearCliente() throws Exception {
        Map<String, Object> request = Map.of(
                "tipoIdentificacion", "CC",
                "numeroIdentificacion", numeroIdentificacionUnico(),
                "nombres", "Camila",
                "apellido", "Torres",
                "correoElectronico", correoUnico(),
                "fechaNacimiento", LocalDate.now().minusYears(30).toString());

        String body = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String crearCuenta(String clienteId, String tipoCuenta, boolean exentaGmf) throws Exception {
        String body = mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("tipoCuenta", tipoCuenta, "clienteId", clienteId, "exentaGmf", exentaGmf))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String numeroIdentificacionUnico() {
        return "ID" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String correoUnico() {
        return "test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
