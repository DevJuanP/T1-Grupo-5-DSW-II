package com.paygo.recargas.dto;

import java.time.LocalDateTime;

// Espejo de T1-DAW II/.../rabbitmq/StockReserveEvent.java (record serializado a JSON).
// Lleva los 5 campos de la solicitud de recarga que consumirá ms-riesgo.
public record RecargaMessage(
        Long idRecarga,
        Long idTarjeta,
        Double saldoDisponible,
        Double montoRecarga,
        LocalDateTime fechaRecarga
) {
}
