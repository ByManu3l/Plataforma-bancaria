package com.bancaria.prueba.common.exception;

/**
 * Señala la violación de una regla de negocio (no un error técnico),
 * por ejemplo: eliminar un cliente con productos vinculados o retirar
 * más saldo del disponible en una cuenta de ahorros.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
