package com.bancaria.prueba.cuenta;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera números de cuenta únicos de 10 dígitos con el prefijo correspondiente
 * al tipo de cuenta (53 ahorros / 33 corriente). Aislar esta responsabilidad
 * (patrón Factory/Generator) permite reutilizarla y probarla de forma independiente.
 */
@Component
@RequiredArgsConstructor
public class NumeroCuentaGenerator {

    private static final int LONGITUD_TOTAL = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CuentaRepository cuentaRepository;

    public String generar(TipoCuenta tipoCuenta) {
        String prefijo = tipoCuenta.getPrefijo();
        int digitosRestantes = LONGITUD_TOTAL - prefijo.length();
        String numeroCuenta;
        do {
            numeroCuenta = prefijo + generarDigitos(digitosRestantes);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    private String generarDigitos(int cantidad) {
        StringBuilder sb = new StringBuilder(cantidad);
        for (int i = 0; i < cantidad; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
