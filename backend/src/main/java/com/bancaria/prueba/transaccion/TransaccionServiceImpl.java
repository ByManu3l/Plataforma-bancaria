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
import com.bancaria.prueba.transaccion.operacion.OperacionCuentaRegistry;
import com.bancaria.prueba.transaccion.operacion.TipoOperacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransaccionServiceImpl implements TransaccionService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final OperacionCuentaRegistry operaciones;
    private final GmfCalculator gmfCalculator;
    private final MovimientoMapper movimientoMapper;
    private final CuentaMapper cuentaMapper;

    @Override
    public MovimientoResponse consignar(ConsignacionRequest request) {
        Cuenta cuenta = obtenerCuentaActiva(request.cuentaId());

        operaciones.para(TipoOperacion.CREDITO).aplicar(cuenta, request.valor());
        cuentaRepository.saveAndFlush(cuenta);

        Movimiento movimiento = registrarMovimiento(
                cuenta, TipoMovimiento.CONSIGNACION, request.valor(), null, null);

        return movimientoMapper.toResponse(movimiento);
    }

    @Override
    public MovimientoResponse retirar(RetiroRequest request) {
        Cuenta cuenta = obtenerCuentaActiva(request.cuentaId());

        operaciones.para(TipoOperacion.DEBITO).aplicar(cuenta, request.valor());
        Movimiento retiro = registrarMovimiento(
                cuenta, TipoMovimiento.RETIRO, request.valor(), null, null);

        cobrarGmf(cuenta, request.valor(), "Retiro por " + request.valor());

        cuentaRepository.saveAndFlush(cuenta);
        return movimientoMapper.toResponse(retiro);
    }

    @Override
    public MovimientoResponse transferir(TransferenciaRequest request) {
        if (request.cuentaOrigenId().equals(request.cuentaDestinoId())) {
            throw new BusinessRuleException("La cuenta origen y la cuenta destino no pueden ser la misma");
        }

        Cuenta origen = obtenerCuentaActiva(request.cuentaOrigenId());
        Cuenta destino = obtenerCuentaActiva(request.cuentaDestinoId());

        operaciones.para(TipoOperacion.DEBITO).aplicar(origen, request.valor());
        operaciones.para(TipoOperacion.CREDITO).aplicar(destino, request.valor());

        Movimiento movimientoEnviado = registrarMovimiento(origen, TipoMovimiento.TRANSFERENCIA_ENVIADA,
                request.valor(), destino, "Transferencia a la cuenta " + destino.getNumeroCuenta());
        registrarMovimiento(destino, TipoMovimiento.TRANSFERENCIA_RECIBIDA,
                request.valor(), origen, "Transferencia recibida de la cuenta " + origen.getNumeroCuenta());

        cobrarGmf(origen, request.valor(), "Transferencia a la cuenta " + destino.getNumeroCuenta());

        cuentaRepository.saveAndFlush(origen);
        cuentaRepository.saveAndFlush(destino);

        return movimientoMapper.toResponse(movimientoEnviado);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoCuentaResponse obtenerEstadoCuenta(UUID cuentaId) {
        Cuenta cuenta = obtenerEntidad(cuentaId);

        List<MovimientoResponse> movimientos = movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId).stream()
                .map(movimientoMapper::toResponse)
                .toList();

        return new EstadoCuentaResponse(cuentaMapper.toResponse(cuenta), movimientos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> listarMovimientos(UUID cuentaId) {
        List<Movimiento> movimientos = cuentaId != null
                ? movimientoRepository.findByCuentaIdOrderByFechaDesc(cuentaId)
                : movimientoRepository.findAllByOrderByFechaDesc();
        return movimientos.stream().map(movimientoMapper::toResponse).toList();
    }

    /**
     * Cobra el GMF sobre una operación débito, cuando la cuenta no está exenta.
     * Se aplica como un débito adicional para que la validación de saldo de las
     * cuentas de ahorro cubra también el gravamen, y se registra como movimiento
     * independiente para que quede trazado en el estado de cuenta.
     */
    private void cobrarGmf(Cuenta cuenta, BigDecimal valorOperacion, String concepto) {
        BigDecimal gmf = gmfCalculator.calcular(cuenta, valorOperacion);
        if (gmf.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        operaciones.para(TipoOperacion.DEBITO).aplicar(cuenta, gmf);
        registrarMovimiento(cuenta, TipoMovimiento.GMF, gmf, null, "GMF (4x1000) — " + concepto);
    }

    /**
     * Registra el movimiento tomando el saldo vigente de la cuenta en ese
     * instante, de modo que en operaciones con más de un efecto sobre la misma
     * cuenta (por ejemplo retiro + GMF) cada movimiento refleje el saldo
     * disponible inmediatamente después de aplicarse.
     */
    private Movimiento registrarMovimiento(Cuenta cuenta, TipoMovimiento tipo, BigDecimal valor,
                                           Cuenta cuentaRelacionada, String descripcion) {
        return movimientoRepository.save(Movimiento.builder()
                .cuenta(cuenta)
                .tipoMovimiento(tipo)
                .valor(valor)
                .saldoDisponible(cuenta.getSaldo())
                .cuentaRelacionada(cuentaRelacionada)
                .descripcion(descripcion)
                .build());
    }

    private Cuenta obtenerCuentaActiva(UUID cuentaId) {
        Cuenta cuenta = obtenerEntidad(cuentaId);
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new BusinessRuleException(
                    "La cuenta " + cuenta.getNumeroCuenta() + " no está activa y no puede recibir transacciones");
        }
        return cuenta;
    }

    private Cuenta obtenerEntidad(UUID cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cuenta con id " + cuentaId));
    }
}
