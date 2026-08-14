package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.cuenta.Cuenta;

import java.math.BigDecimal;

/**
 * Estrategia que encapsula el efecto de un movimiento sobre el saldo de una
 * cuenta (crédito o débito). El servicio de transacciones depende únicamente de
 * esta abstracción y resuelve la implementación concreta a través de
 * {@link OperacionCuentaRegistry}, por lo que agregar un nuevo efecto sobre el
 * saldo consiste en publicar una nueva implementación como bean, sin modificar
 * la lógica ya existente (principio Open/Closed y de inversión de dependencias).
 *
 * <p>Tanto la consignación como el lado receptor de una transferencia reutilizan
 * la estrategia de crédito; el retiro, el cobro del GMF y el lado emisor de una
 * transferencia reutilizan la de débito.
 */
public interface OperacionCuenta {

    /**
     * Efecto sobre el saldo que implementa esta estrategia.
     */
    TipoOperacion tipo();

    /**
     * Aplica el efecto sobre el saldo de la cuenta, validando las reglas de
     * negocio propias del movimiento (por ejemplo, saldo suficiente).
     */
    void aplicar(Cuenta cuenta, BigDecimal valor);
}
