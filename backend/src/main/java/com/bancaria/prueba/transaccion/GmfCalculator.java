package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.cuenta.Cuenta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula el Gravamen a los Movimientos Financieros (GMF, "4x1000") que se cobra
 * sobre las operaciones débito de una cuenta.
 *
 * <p>Da sentido al atributo {@code exentaGmf} del producto financiero: las cuentas
 * marcadas como exentas no pagan el gravamen. La tarifa es configurable
 * ({@code app.gmf.tarifa}) para no dejarla incrustada en el código, ya que es un
 * parámetro tributario que puede cambiar por ley.
 */
@Component
public class GmfCalculator {

    private final BigDecimal tarifa;

    public GmfCalculator(@Value("${app.gmf.tarifa:0.004}") BigDecimal tarifa) {
        this.tarifa = tarifa;
    }

    /**
     * @return el GMF a cobrar sobre {@code valor}, o cero si la cuenta está exenta.
     */
    public BigDecimal calcular(Cuenta cuenta, BigDecimal valor) {
        if (cuenta.isExentaGmf()) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(tarifa).setScale(2, RoundingMode.HALF_UP);
    }
}
