package com.bancaria.prueba.cliente;

import com.bancaria.prueba.cliente.dto.ClienteRequest;
import com.bancaria.prueba.cliente.dto.ClienteResponse;
import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.common.exception.ResourceNotFoundException;
import com.bancaria.prueba.cuenta.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;

    @Override
    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.existsByNumeroIdentificacion(request.numeroIdentificacion())) {
            throw new BusinessRuleException(
                    "Ya existe un cliente registrado con el número de identificación " + request.numeroIdentificacion());
        }
        if (clienteRepository.existsByCorreoElectronico(request.correoElectronico())) {
            throw new BusinessRuleException(
                    "Ya existe un cliente registrado con el correo " + request.correoElectronico());
        }
        Cliente cliente = clienteMapper.toEntity(request);
        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.toResponse(guardado);
    }

    @Override
    public ClienteResponse actualizar(UUID id, ClienteRequest request) {
        Cliente cliente = obtenerEntidad(id);

        if (!cliente.getNumeroIdentificacion().equals(request.numeroIdentificacion())
                && clienteRepository.existsByNumeroIdentificacion(request.numeroIdentificacion())) {
            throw new BusinessRuleException(
                    "Ya existe un cliente registrado con el número de identificación " + request.numeroIdentificacion());
        }
        if (!cliente.getCorreoElectronico().equals(request.correoElectronico())
                && clienteRepository.existsByCorreoElectronico(request.correoElectronico())) {
            throw new BusinessRuleException(
                    "Ya existe un cliente registrado con el correo " + request.correoElectronico());
        }

        clienteMapper.actualizarEntidad(request, cliente);
        Cliente actualizado = clienteRepository.save(cliente);
        return clienteMapper.toResponse(actualizado);
    }

    @Override
    public void eliminar(UUID id) {
        Cliente cliente = obtenerEntidad(id);
        if (cuentaRepository.existsByClienteId(cliente.getId())) {
            throw new BusinessRuleException(
                    "No es posible eliminar el cliente porque tiene productos financieros vinculados");
        }
        clienteRepository.delete(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtener(UUID id) {
        return clienteMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    private Cliente obtenerEntidad(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cliente con id " + id));
    }
}
