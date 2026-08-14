package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.cliente.Cliente;
import com.bancaria.prueba.cliente.ClienteRepository;
import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.dto.ActualizarCuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final NumeroCuentaGenerator numeroCuentaGenerator;
    private final CuentaMapper cuentaMapper;

    @Override
    public CuentaResponse crear(CuentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cliente con id " + request.clienteId()));

        Cuenta cuenta = Cuenta.builder()
                .tipoCuenta(request.tipoCuenta())
                .numeroCuenta(numeroCuentaGenerator.generar(request.tipoCuenta()))
                .estado(EstadoCuenta.ACTIVA)
                .saldo(BigDecimal.ZERO)
                .exentaGmf(request.exentaGmf())
                .cliente(cliente)
                .build();

        Cuenta guardada = cuentaRepository.save(cuenta);
        return cuentaMapper.toResponse(guardada);
    }

    @Override
    public CuentaResponse cambiarEstado(UUID id, EstadoCuenta nuevoEstado) {
        Cuenta cuenta = obtenerEntidad(id);

        if (cuenta.getEstado() == EstadoCuenta.CANCELADA) {
            throw new BusinessRuleException(
                    "La cuenta " + cuenta.getNumeroCuenta() + " está cancelada; la cancelación es definitiva");
        }
        if (nuevoEstado == EstadoCuenta.CANCELADA && cuenta.getSaldo().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0");
        }

        cuenta.setEstado(nuevoEstado);
        // saveAndFlush fuerza el UPDATE dentro de la transacción para que la
        // devolución de @PreUpdate (fechaModificacion) quede reflejada en la respuesta.
        Cuenta actualizada = cuentaRepository.saveAndFlush(cuenta);
        return cuentaMapper.toResponse(actualizada);
    }

    @Override
    public CuentaResponse actualizar(UUID id, ActualizarCuentaRequest request) {
        Cuenta cuenta = obtenerEntidad(id);

        if (cuenta.getEstado() == EstadoCuenta.CANCELADA) {
            throw new BusinessRuleException(
                    "No es posible modificar la cuenta " + cuenta.getNumeroCuenta() + " porque está cancelada");
        }

        cuenta.setExentaGmf(request.exentaGmf());
        Cuenta actualizada = cuentaRepository.saveAndFlush(cuenta);
        return cuentaMapper.toResponse(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtener(UUID id) {
        return cuentaMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> listar() {
        return cuentaRepository.findAll().stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> listarPorCliente(UUID clienteId) {
        return cuentaRepository.findByClienteId(clienteId).stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    private Cuenta obtenerEntidad(UUID id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cuenta con id " + id));
    }
}
