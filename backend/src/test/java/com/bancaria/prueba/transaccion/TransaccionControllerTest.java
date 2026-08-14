package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import com.bancaria.prueba.transaccion.dto.ConsignacionRequest;
import com.bancaria.prueba.transaccion.dto.EstadoCuentaResponse;
import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import com.bancaria.prueba.transaccion.dto.RetiroRequest;
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransaccionController.class)
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TransaccionService transaccionService;

    private final UUID cuentaId = UUID.randomUUID();

    @Test
    void consignar_deberiaRetornar200_cuandoElValorEsValido() throws Exception {
        ConsignacionRequest request = new ConsignacionRequest(cuentaId, new BigDecimal("50.00"));
        when(transaccionService.consignar(any(ConsignacionRequest.class))).thenReturn(
                new MovimientoResponse(UUID.randomUUID(), TipoMovimiento.CONSIGNACION, request.valor(),
                        new BigDecimal("150.00"), null, cuentaId, "5300000001", null, null, null));

        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMovimiento").value("CONSIGNACION"))
                .andExpect(jsonPath("$.saldoDisponible").value(150.00));
    }

    @Test
    void consignar_deberiaRetornar400_cuandoElValorEsCero() throws Exception {
        String cuerpoInvalido = """
                { "cuentaId": "%s", "valor": 0 }
                """.formatted(cuentaId);

        mockMvc.perform(post("/api/transacciones/consignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirar_deberiaRetornar422_cuandoElSaldoEsInsuficiente() throws Exception {
        RetiroRequest request = new RetiroRequest(cuentaId, new BigDecimal("1000.00"));
        when(transaccionService.retirar(any(RetiroRequest.class)))
                .thenThrow(new BusinessRuleException("Saldo insuficiente: la cuenta de ahorros 5300000001 no puede quedar con saldo negativo"));

        mockMvc.perform(post("/api/transacciones/retiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transferir_deberiaRetornar200_cuandoLasCuentasSonValidas() throws Exception {
        UUID destinoId = UUID.randomUUID();
        TransferenciaRequest request = new TransferenciaRequest(cuentaId, destinoId, new BigDecimal("30.00"));
        when(transaccionService.transferir(any(TransferenciaRequest.class))).thenReturn(
                new MovimientoResponse(UUID.randomUUID(), TipoMovimiento.TRANSFERENCIA_ENVIADA, request.valor(),
                        new BigDecimal("70.00"), null, cuentaId, "5300000001", destinoId, "3300000001", null));

        mockMvc.perform(post("/api/transacciones/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMovimiento").value("TRANSFERENCIA_ENVIADA"))
                .andExpect(jsonPath("$.cuentaRelacionadaId").value(destinoId.toString()));
    }

    @Test
    void obtenerEstadoCuenta_deberiaRetornar200_conSaldoYMovimientos() throws Exception {
        CuentaResponse cuenta = new CuentaResponse(cuentaId, TipoCuenta.AHORROS, "5300000001", EstadoCuenta.ACTIVA,
                new BigDecimal("100.00"), new BigDecimal("100.00"), true, null, null, UUID.randomUUID(), "Laura Gómez");
        when(transaccionService.obtenerEstadoCuenta(eq(cuentaId)))
                .thenReturn(new EstadoCuentaResponse(cuenta, List.of()));

        mockMvc.perform(get("/api/transacciones/cuentas/{cuentaId}/estado", cuentaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.saldo").value(100.00))
                .andExpect(jsonPath("$.movimientos").isArray());
    }
}
