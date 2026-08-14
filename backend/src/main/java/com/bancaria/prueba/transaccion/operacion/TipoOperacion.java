package com.bancaria.prueba.transaccion.operacion;

/**
 * Efecto que una operación produce sobre el saldo de una cuenta. Sirve como
 * clave para seleccionar la estrategia correspondiente en tiempo de ejecución.
 */
public enum TipoOperacion {
    CREDITO,
    DEBITO
}
