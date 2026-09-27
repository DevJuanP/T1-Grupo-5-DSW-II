package com.paygo.riesgo.consumer;

import com.paygo.riesgo.config.RabbitMQConfig;
import com.paygo.riesgo.dto.RecargaMessage;
import com.paygo.riesgo.service.AnalisisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Espejo de T1-DAW II/backend/products-services/.../rabbitmq/StockReserveConsumer.java
// (@Component + @RabbitListener + delegación al @Service + log).
@Component
public class RiesgoConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiesgoConsumer.class);

    private final AnalisisService analisisService;

    public RiesgoConsumer(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @RabbitListener(queues = RabbitMQConfig.ATUNCAR_QUEUE)
    public void onRecarga(RecargaMessage message) {
        var guardado = analisisService.registrar(message);
        LOGGER.info("[ms-riesgo] Recarga evaluada. idRecarga={}, idTarjeta={}, situacion={}",
                guardado.getIdRecarga(), guardado.getIdTarjeta(), guardado.getSituacion());
    }
}
