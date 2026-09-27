package com.paygo.recargas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Espejo de T1-DAW II/backend/sales-services/.../rabbitmq/RabbitMQConfig.java.
// Declara la misma cola logica del examen (atuncar_queue) del lado productor.
// Kafka (KafkaTopicConfig) sigue activo: modo dual `both` publica en ambos.
@Configuration
public class RabbitMQConfig {

    public static final String PAYGO_EXCHANGE = "paygo-exchange";
    public static final String ATUNCAR_QUEUE = "atuncar_queue";
    public static final String ROUTING_KEY = "atuncar.routing";

    @Bean
    public DirectExchange paygoExchange() {
        return new DirectExchange(PAYGO_EXCHANGE);
    }

    @Bean
    public Queue atuncarQueue() {
        return new Queue(ATUNCAR_QUEUE, true);
    }

    @Bean
    public Binding atuncarBinding(Queue atuncarQueue, DirectExchange paygoExchange) {
        return BindingBuilder.bind(atuncarQueue)
                .to(paygoExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
