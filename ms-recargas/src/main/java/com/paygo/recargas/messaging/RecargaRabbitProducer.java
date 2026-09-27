package com.paygo.recargas.messaging;

import com.paygo.recargas.config.RabbitMQConfig;
import com.paygo.recargas.dto.RecargaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Espejo de T1-DAW II/.../rabbitmq/StockReserveProducer.java.
// Publica el mismo RecargaMessage que el producer Kafka, pero via RabbitMQ.
@Component
public class RecargaRabbitProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecargaRabbitProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RecargaRabbitProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(RecargaMessage event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYGO_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event);
        LOGGER.info("[ms-recargas-rabbit] Evento Rabbit publicado. exchange={}, routingKey={}, payload={}",
                RabbitMQConfig.PAYGO_EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }
}
