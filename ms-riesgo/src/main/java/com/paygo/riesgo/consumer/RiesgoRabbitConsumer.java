package com.paygo.riesgo.consumer;

import com.paygo.riesgo.dto.RecargaMessage;
import com.paygo.riesgo.service.AnalisisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Espejo de T1-DAW II/.../rabbitmq/StockReserveConsumer.java (@RabbitListener + log).
// Consume la misma cola logica atuncar_queue que el consumer Kafka (RiesgoConsumer).
// La deduplicacion la hace AnalisisService.evaluar (idempotente por idRecarga).
@Component
public class RiesgoRabbitConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiesgoRabbitConsumer.class);

    private final AnalisisService analisisService;

    public RiesgoRabbitConsumer(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @RabbitListener(queues = "atuncar_queue")
    public void onRecarga(RecargaMessage message) {
        var guardado = analisisService.evaluar(message);
        LOGGER.info("[ms-riesgo-rabbit] Recarga evaluada. idRecarga={}, idTarjeta={}, situacion={}",
                guardado.getIdRecarga(), guardado.getIdTarjeta(), guardado.getSituacion());
    }
}
