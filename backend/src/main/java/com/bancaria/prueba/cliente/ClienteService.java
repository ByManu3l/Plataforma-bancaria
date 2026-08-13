package com.bancaria.prueba.cliente;

import com.bancaria.prueba.cliente.dto.ClienteRequest;
import com.bancaria.prueba.cliente.dto.ClienteResponse;

import java.util.List;
import java.util.UUID;

public interface ClienteService {

    ClienteResponse crear(ClienteRequest request);

    ClienteResponse actualizar(UUID id, ClienteRequest request);

    void eliminar(UUID id);

    ClienteResponse obtener(UUID id);

    List<ClienteResponse> listar();
}
