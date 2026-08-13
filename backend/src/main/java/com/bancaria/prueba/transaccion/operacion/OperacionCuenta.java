package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.cuenta.Cuenta;

import java.math.BigDecimal;

/**
 * Estrategia que encapsula el efecto de un movimiento sobre el saldo de una
 * cuenta (crédito o débito). Permite agregar nuevos efectos (patrón Strategy)
 * sin modificar el servicio de transacciones (principio Open/Closed); tanto
 * la consignación como el lado receptor de una transferencia reutilizan la
 * misma estrategia de crédito, y el retiro con el lado emisor de una
 * transferencia reutilizan la misma estrategia de débito.
 */
public interface OperacionCuenta {

    /**
     * Aplica el efecto sobre el saldo de la cuenta, validando las reglas de
     * negocio propias del movimiento (por ejemplo, saldo suficiente).
     */
    void aplicar(Cuenta cuenta, BigDecimal valor);
}
