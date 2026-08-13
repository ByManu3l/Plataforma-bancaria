package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.transaccion.dto.ConsignacionRequest;
import com.bancaria.prueba.transaccion.dto.EstadoCuentaResponse;
import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import com.bancaria.prueba.transaccion.dto.RetiroRequest;
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;

import java.util.UUID;

public interface TransaccionService {

    MovimientoResponse consignar(ConsignacionRequest request);

    MovimientoResponse retirar(RetiroRequest request);

    MovimientoResponse transferir(TransferenciaRequest request);

    EstadoCuentaResponse obtenerEstadoCuenta(UUID cuentaId);
}
