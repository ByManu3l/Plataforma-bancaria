package com.bancaria.prueba.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    boolean existsByCorreoElectronico(String correoElectronico);

    Optional<Cliente> findByNumeroIdentificacion(String numeroIdentificacion);
}
