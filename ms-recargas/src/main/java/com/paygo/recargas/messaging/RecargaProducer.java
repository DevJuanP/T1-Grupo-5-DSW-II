package com.paygo.recargas.messaging;

import com.paygo.recargas.config.KafkaTopicConfig;
import com.paygo.recargas.dto.RecargaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RecargaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecargaProducer.class);

    private final KafkaTemplate<String, RecargaMessage> kafkaTemplate;

    public RecargaProducer(KafkaTemplate<String, RecargaMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RecargaMessage event) {
        kafkaTemplate.send(KafkaTopicConfig.ATUNCAR_TOPIC, event.idRecarga().toString(), event);
        LOGGER.info("Evento Kafka publicado. topic={}, key={}, payload={}",
                KafkaTopicConfig.ATUNCAR_TOPIC, event.idRecarga(), event);
    }
}
