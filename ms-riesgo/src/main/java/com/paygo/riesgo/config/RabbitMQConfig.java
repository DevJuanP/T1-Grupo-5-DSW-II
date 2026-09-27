package com.paygo.riesgo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Espejo de T1-DAW II/backend/products-services/.../rabbitmq/RabbitMQConfig.java
// La guía declara la infra en ambos lados (sales y products); aquí igual:
// mismas constantes que ms-recargas para atar la cola atuncar_queue.
// (Fase 5 del plan)
@Configuration
public class RabbitMQConfig {

    public static final String PAYGO_EXCHANGE = "paygo-exchange";
    public static final String ATUNCAR_QUEUE = "atuncar_queue";
    public static final String ATUNCAR_ROUTING_KEY = "atuncar.routing";

    @Bean
    public DirectExchange paygoExchange() {
        return new DirectExchange(PAYGO_EXCHANGE);
    }

    @Bean
    public Queue atuncarQueue() {
        return new Queue(ATUNCAR_QUEUE);
    }

    @Bean
    public Binding atuncarBinding(Queue atuncarQueue, DirectExchange paygoExchange) {
        return BindingBuilder.bind(atuncarQueue)
                .to(paygoExchange)
                .with(ATUNCAR_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
