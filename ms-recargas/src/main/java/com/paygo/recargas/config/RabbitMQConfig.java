package com.paygo.recargas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
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

    // Mapper Jackson 2 explícito con JavaTimeModule (fechas ISO-8601, no timestamps)
    // y paquetes confiables: por defecto el type mapper solo confía en
    // java.util/java.lang y rechazaría com.paygo.* al consumir.
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
