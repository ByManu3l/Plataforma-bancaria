package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovimientoMapper {

    @Mapping(target = "cuentaId", source = "cuenta.id")
    @Mapping(target = "numeroCuenta", source = "cuenta.numeroCuenta")
    @Mapping(target = "cuentaRelacionadaId", source = "cuentaRelacionada.id")
    @Mapping(target = "numeroCuentaRelacionada", source = "cuentaRelacionada.numeroCuenta")
    MovimientoResponse toResponse(Movimiento movimiento);
}
