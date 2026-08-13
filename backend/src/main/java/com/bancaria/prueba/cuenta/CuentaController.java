package com.bancaria.prueba.cuenta;

import com.bancaria.prueba.cuenta.dto.CambiarEstadoCuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaRequest;
import com.bancaria.prueba.cuenta.dto.CuentaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Administración de productos financieros (cuentas de ahorro y corriente)")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    @Operation(summary = "Crear una cuenta de ahorros o corriente para un cliente existente")
    public ResponseEntity<CuentaResponse> crear(@Valid @RequestBody CuentaRequest request) {
        CuentaResponse creada = cuentaService.crear(request);
        return ResponseEntity.created(URI.create("/api/cuentas/" + creada.id())).body(creada);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar, inactivar o cancelar una cuenta")
    public ResponseEntity<CuentaResponse> cambiarEstado(@PathVariable UUID id,
                                                          @Valid @RequestBody CambiarEstadoCuentaRequest request) {
        return ResponseEntity.ok(cuentaService.cambiarEstado(id, request.estado()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cuenta por id")
    public ResponseEntity<CuentaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar cuentas, opcionalmente filtradas por cliente")
    public ResponseEntity<List<CuentaResponse>> listar(@RequestParam(required = false) UUID clienteId) {
        List<CuentaResponse> cuentas = clienteId != null
                ? cuentaService.listarPorCliente(clienteId)
                : cuentaService.listar();
        return ResponseEntity.ok(cuentas);
    }
}
