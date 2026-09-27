package com.paygo.riesgo.config;

import com.paygo.riesgo.dto.RecargaMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

// Spring Boot 4 ya no trae auto-config de Kafka (no hay KafkaAutoConfiguration
// en el conditions report): sin @EnableKafka + estas factories explícitas el
// @KafkaListener de RiesgoConsumer queda inerte. Patrón espejo del producer
// explícito de ms-recargas/.../config/KafkaTopicConfig.java.
// Reutiliza las mismas claves de application.properties (bootstrap, group-id...).
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, RecargaMessage> riesgoConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RecargaMessage.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.paygo.riesgo.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RecargaMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, RecargaMessage> riesgoConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, RecargaMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(riesgoConsumerFactory);
        return factory;
    }
}
