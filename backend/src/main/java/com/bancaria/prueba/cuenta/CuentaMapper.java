package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CuentaMapper {

    @Mapping(target = "saldoDisponible", source = "saldo")
    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombreCompleto", expression = "java(cuenta.getCliente().getNombres() + \" \" + cuenta.getCliente().getApellido())")
    CuentaResponse toResponse(Cuenta cuenta);
}
