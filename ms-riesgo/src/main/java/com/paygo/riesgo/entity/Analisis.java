package com.paygo.riesgo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Tabla analisis del examen (Fase 4 del plan).
// Espejo de Tarjeta/Recarga: @Entity + @Data + @NoArgs/@AllArgs.
@Entity
@Table(name = "analisis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analisis {

    @Id
    private Long idRecarga;
    private Long idTarjeta;
    private Double saldoDisponible;
    private Double montoRecarga;
    private LocalDateTime fechaRecarga;
    private String situacion;
}
