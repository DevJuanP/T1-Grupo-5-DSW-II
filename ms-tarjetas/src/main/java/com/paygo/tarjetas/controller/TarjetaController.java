package com.paygo.tarjetas.controller;
import com.paygo.tarjetas.entity.Tarjeta;
import com.paygo.tarjetas.service.TarjetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/tarjetas")
public class TarjetaController {
    private final TarjetaService tarjetaService;
    public TarjetaController(TarjetaService tarjetaService) {
        this.tarjetaService = tarjetaService;
    }
    @PostMapping
    public ResponseEntity<Tarjeta> registrar(
            @RequestBody Tarjeta tarjeta) {
        return ResponseEntity.ok(
                tarjetaService.registrar(tarjeta)
        );
    }
    @GetMapping
    public ResponseEntity<List<Tarjeta>> listar() {
        return ResponseEntity.ok(
                tarjetaService.listar()
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<Tarjeta> buscarPorId(
            @PathVariable Long id) {
        return tarjetaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
