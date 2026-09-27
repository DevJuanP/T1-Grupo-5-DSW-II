package com.paygo.riesgo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Espejo de T1-DAW II/backend/products-services/.../rabbitmq/RabbitMQConfig.java
// y del lado productor (ms-recargas/.../config/RabbitMQConfig.java).
// Mismas constantes para que el binding declare la misma cola atuncar_queue.
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

    // Idéntico al productor (ms-recargas): mismo mapper ISO + mismos paquetes
    // confiables para deserializar RecargaMessage desde atuncar_queue.
    @Bean
    public MessageConverter jsonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.paygo.*", "java.util", "java.lang");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
