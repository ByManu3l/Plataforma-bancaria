package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.CuentaMapper;
import com.bancaria.prueba.cuenta.CuentaRepository;
import com.bancaria.prueba.cuenta.EstadoCuenta;
import com.bancaria.prueba.transaccion.dto.ConsignacionRequest;
import com.bancaria.prueba.transaccion.dto.EstadoCuentaResponse;
import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import com.bancaria.prueba.transaccion.dto.RetiroRequest;
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;
import com.bancaria.prueba.transaccion.operacion.CreditoOperacion;
import com.bancaria.prueba.transaccion.operacion.DebitoOperacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransaccionServiceImpl implements TransaccionService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final CreditoOperacion creditoOperacion;
    private final DebitoOperacion debitoOperacion;
    private final MovimientoMapper movimientoMapper;
    private final CuentaMapper cuentaMapper;

    @Override
    public MovimientoResponse consignar(ConsignacionRequest request) {
        Cuenta cuenta = obtenerCuentaActiva(request.cuentaId());

        creditoOperacion.aplicar(cuenta, request.valor());
        cuentaRepository.save(cuenta);

        Movimiento movimiento = Movimiento.builder()
                .cuenta(cuenta)
                .tipoMovimiento(TipoMovimiento.CONSIGNACION)
                .valor(request.valor())
                .saldoDisponible(cuenta.getSaldo())
                .build();

        return movimientoMapper.toResponse(movimientoRepository.save(movimiento));
    }

    @Override
    public MovimientoResponse retirar(RetiroRequest request) {
        Cuenta cuenta = obtenerCuentaActiva(request.cuentaId());

        debitoOperacion.aplicar(cuenta, request.valor());
        cuentaRepository.save(cuenta);

        Movimiento movimiento = Movimiento.builder()
                .cuenta(cuenta)
                .tipoMovimiento(TipoMovimiento.RETIRO)
                .valor(request.valor())
                .saldoDisponible(cuenta.getSaldo())
                .build();

        return movimientoMapper.toResponse(movimientoRepository.save(movimiento));
    }

    @Override
    public MovimientoResponse transferir(TransferenciaRequest request) {
        if (request.cuentaOrigenId().equals(request.cuentaDestinoId())) {
            throw new BusinessRuleException("La cuenta origen y la cuenta destino no pueden ser la misma");
        }

        Cuenta origen = obtenerCuentaActiva(request.cuentaOrigenId());
        Cuenta destino = obtenerCuentaActiva(request.cuentaDestinoId());

        debitoOperacion.aplicar(origen, request.valor());
        creditoOperacion.aplicar(destino, request.valor());
        cuentaRepository.save(origen);
        cuentaRepository.save(destino);

        Movimiento movimientoEnviado = Movimiento.builder()
                .cuenta(origen)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA_ENVIADA)
                .valor(request.valor())
                .saldoDisponible(origen.getSaldo())
                .cuentaRelacionada(destino)
                .descripcion("Transferencia a la cuenta " + destino.getNumeroCuenta())
                .build();
        Movimiento movimientoRecibido = Movimiento.builder()
                .cuenta(destino)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA_RECIBIDA)
                .valor(request.valor())
                .saldoDisponible(destino.getSaldo())
                .cuentaRelacionada(origen)
                .descripcion("Transferencia recibida de la cuenta " + origen.getNumeroCuenta())
                .build();

        movimientoRepository.save(movimientoRecibido);
        Movimiento guardado = movimientoRepository.save(movimientoEnviado);

        return movimientoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoCuentaResponse obtenerEstadoCuenta(UUID cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cuenta con id " + cuentaId));

        var movimientos = movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .map(movimientoMapper::toResponse)
                .toList();

        return new EstadoCuentaResponse(cuentaMapper.toResponse(cuenta), movimientos);
    }

    private Cuenta obtenerCuentaActiva(UUID cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cuenta con id " + cuentaId));
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new BusinessRuleException(
                    "La cuenta " + cuenta.getNumeroCuenta() + " no está activa y no puede recibir transacciones");
        }
        return cuenta;
    }
}
