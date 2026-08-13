package com.bancaria.prueba.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

    boolean existsByClienteId(UUID clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteId(UUID clienteId);
}
