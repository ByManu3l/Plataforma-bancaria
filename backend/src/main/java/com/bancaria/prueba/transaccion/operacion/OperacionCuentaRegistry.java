package com.bancaria.prueba.transaccion.operacion;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de las estrategias {@link OperacionCuenta} disponibles, indexadas por
 * el efecto que producen sobre el saldo.
 *
 * <p>Spring inyecta todas las implementaciones publicadas como bean, de modo que
 * incorporar un nuevo efecto no requiere modificar esta clase ni el servicio de
 * transacciones: basta con publicar la nueva implementación.
 */
@Component
public class OperacionCuentaRegistry {

    private final Map<TipoOperacion, OperacionCuenta> operaciones = new EnumMap<>(TipoOperacion.class);

    public OperacionCuentaRegistry(List<OperacionCuenta> implementaciones) {
        implementaciones.forEach(operacion -> operaciones.put(operacion.tipo(), operacion));
    }

    public OperacionCuenta para(TipoOperacion tipo) {
        OperacionCuenta operacion = operaciones.get(tipo);
        if (operacion == null) {
            throw new IllegalStateException("No hay una estrategia registrada para la operación " + tipo);
        }
        return operacion;
    }
}
