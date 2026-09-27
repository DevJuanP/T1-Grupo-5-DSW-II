package com.paygo.recargas.controller;
import com.paygo.recargas.entity.Recarga;
import com.paygo.recargas.service.RecargaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/recargas")
public class RecargaController {
    private final RecargaService recargaService;
    public RecargaController(RecargaService recargaService) {
        this.recargaService = recargaService;
    }
    @PostMapping
    public ResponseEntity<Recarga> registrar(
            @Valid @RequestBody Recarga recarga) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                recargaService.registrar(recarga)
        );
    }
    @GetMapping
    public ResponseEntity<List<Recarga>> listar() {
        return ResponseEntity.ok(
                recargaService.listar()
        );
    }
}