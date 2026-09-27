package com.paygo.recargas.messaging;

import com.paygo.recargas.config.RabbitMQConfig;
import com.paygo.recargas.dto.RecargaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Espejo de T1-DAW II/backend/sales-services/.../rabbitmq/StockReserveProducer.java
@Component
public class RecargaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecargaProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RecargaProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(RecargaMessage event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYGO_EXCHANGE,
                RabbitMQConfig.ATUNCAR_ROUTING_KEY,
                event
        );
        LOGGER.info("Evento RabbitMQ publicado. exchange={}, routingKey={}, payload={}",
                RabbitMQConfig.PAYGO_EXCHANGE, RabbitMQConfig.ATUNCAR_ROUTING_KEY, event);
    }
}
