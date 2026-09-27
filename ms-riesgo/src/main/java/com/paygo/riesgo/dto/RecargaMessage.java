package com.paygo.riesgo.dto;

import java.time.LocalDateTime;

// Mismo shape que ms-recargas/.../dto/RecargaMessage.java para deserializar
// el JSON publicado en atuncar_queue (Jackson2JsonMessageConverter).
// Espejo de T1-DAW II/.../rabbitmq/StockReserveEvent.java.
public record RecargaMessage(
        Long idRecarga,
        Long idTarjeta,
        Double saldoDisponible,
        Double montoRecarga,
        LocalDateTime fechaRecarga
) {
}
