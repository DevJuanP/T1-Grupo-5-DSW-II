package com.paygo.riesgo.consumer;

import com.paygo.riesgo.config.KafkaTopicConfig;
import com.paygo.riesgo.dto.RecargaMessage;
import com.paygo.riesgo.service.AnalisisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RiesgoConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiesgoConsumer.class);

    private final AnalisisService analisisService;

    public RiesgoConsumer(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @KafkaListener(topics = KafkaTopicConfig.ATUNCAR_TOPIC)
    public void onRecarga(RecargaMessage message) {
        var guardado = analisisService.evaluar(message);
        LOGGER.info("[ms-riesgo] Recarga evaluada. idRecarga={}, idTarjeta={}, situacion={}",
                guardado.getIdRecarga(), guardado.getIdTarjeta(), guardado.getSituacion());
    }
}
