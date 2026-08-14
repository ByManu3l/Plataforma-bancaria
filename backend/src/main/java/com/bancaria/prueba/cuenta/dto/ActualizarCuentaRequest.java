package com.bancaria.prueba.cuenta.dto;

/**
 * Único atributo de negocio modificable de una cuenta. El tipo, el número y el
 * titular son inmutables por definición del producto financiero, y el saldo solo
 * cambia a través de transacciones.
 */
public record ActualizarCuentaRequest(boolean exentaGmf) {
}
