package com.bancaria.prueba.transaccion;

import com.bancaria.prueba.transaccion.dto.ConsignacionRequest;
import com.bancaria.prueba.transaccion.dto.EstadoCuentaResponse;
import com.bancaria.prueba.transaccion.dto.MovimientoResponse;
import com.bancaria.prueba.transaccion.dto.RetiroRequest;
import com.bancaria.prueba.transaccion.dto.TransferenciaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
@Tag(name = "Transacciones", description = "Movimientos financieros: consignaciones, retiros y transferencias")
public class TransaccionController {

    private final TransaccionService transaccionService;

    @PostMapping("/consignaciones")
    @Operation(summary = "Registrar una consignación en una cuenta")
    public ResponseEntity<MovimientoResponse> consignar(@Valid @RequestBody ConsignacionRequest request) {
        return ResponseEntity.ok(transaccionService.consignar(request));
    }

    @PostMapping("/retiros")
    @Operation(summary = "Registrar un retiro de una cuenta")
    public ResponseEntity<MovimientoResponse> retirar(@Valid @RequestBody RetiroRequest request) {
        return ResponseEntity.ok(transaccionService.retirar(request));
    }

    @PostMapping("/transferencias")
    @Operation(summary = "Transferir saldo entre dos cuentas existentes")
    public ResponseEntity<MovimientoResponse> transferir(@Valid @RequestBody TransferenciaRequest request) {
        return ResponseEntity.ok(transaccionService.transferir(request));
    }

    @GetMapping("/cuentas/{cuentaId}/estado")
    @Operation(summary = "Consultar el estado de cuenta: saldo actual e historial de movimientos")
    public ResponseEntity<EstadoCuentaResponse> obtenerEstadoCuenta(@PathVariable UUID cuentaId) {
        return ResponseEntity.ok(transaccionService.obtenerEstadoCuenta(cuentaId));
    }
}
