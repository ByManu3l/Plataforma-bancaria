package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.common.exception.BusinessRuleException;
import com.bancaria.prueba.cuenta.Cuenta;
import com.bancaria.prueba.cuenta.TipoCuenta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Efecto de débito sobre una cuenta: retiro o lado emisor de una transferencia.
 * Las cuentas de ahorro no pueden quedar con saldo negativo; las cuentas
 * corrientes sí admiten sobregiro, ya que el requerimiento solo restringe el
 * saldo mínimo de las cuentas de ahorro.
 */
@Component
public class DebitoOperacion implements OperacionCuenta {

    @Override
    public void aplicar(Cuenta cuenta, BigDecimal valor) {
        BigDecimal saldoResultante = cuenta.getSaldo().subtract(valor);
        if (cuenta.getTipoCuenta() == TipoCuenta.AHORROS && saldoResultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "Saldo insuficiente: la cuenta de ahorros " + cuenta.getNumeroCuenta() + " no puede quedar con saldo negativo");
        }
        cuenta.setSaldo(saldoResultante);
    }
}
