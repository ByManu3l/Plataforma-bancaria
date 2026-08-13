package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.cuenta.dto.CuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;

import java.util.List;
import java.util.UUID;

public interface CuentaService {

    CuentaResponse crear(CuentaRequest request);

    CuentaResponse cambiarEstado(UUID id, EstadoCuenta nuevoEstado);

    CuentaResponse obtener(UUID id);

    List<CuentaResponse> listar();

    List<CuentaResponse> listarPorCliente(UUID clienteId);
}
