package com.bancaria.prueba.cliente;

/**
 * Documentos admitidos para identificar a un cliente.
 *
 * <p>No se incluye la tarjeta de identidad: en Colombia la expide el registro
 * civil para personas de 7 a 17 años, de modo que sería incompatible con la
 * regla de negocio que exige que todo cliente sea mayor de edad. Admitirla
 * habría dejado en el modelo un valor imposible de usar.
 */
public enum TipoIdentificacion {
    /** Cédula de ciudadanía. */
    CC,
    /** Cédula de extranjería. */
    CE,
    /** Pasaporte. */
    PASAPORTE
}
