package com.paygo.riesgo.controller;

import com.paygo.riesgo.entity.Analisis;
import com.paygo.riesgo.service.AnalisisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Espejo de TarjetaController/RecargaController (estilo PAYGO:
// @RequestMapping + ResponseEntity). El examen exige un controlador
// que permita listar el contenido de la tabla análisis.
@RestController
@RequestMapping("/analisis")
public class AnalisisController {

    private final AnalisisService analisisService;

    public AnalisisController(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @GetMapping
    public ResponseEntity<List<Analisis>> listar() {
        return ResponseEntity.ok(
                analisisService.listar()
        );
    }
}
