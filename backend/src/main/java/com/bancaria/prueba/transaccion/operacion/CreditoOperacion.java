package com.bancaria.prueba.transaccion.operacion;

import com.bancaria.prueba.cuenta.Cuenta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Efecto de crédito sobre una cuenta: consignación o lado receptor de una transferencia.
 */
@Component
public class CreditoOperacion implements OperacionCuenta {

    @Override
    public TipoOperacion tipo() {
        return TipoOperacion.CREDITO;
    }

    @Override
    public void aplicar(Cuenta cuenta, BigDecimal valor) {
        cuenta.setSaldo(cuenta.getSaldo().add(valor));
    }
}
