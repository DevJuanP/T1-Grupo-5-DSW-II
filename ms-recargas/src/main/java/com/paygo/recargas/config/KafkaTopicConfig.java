package com.paygo.recargas.config;

import com.paygo.recargas.dto.RecargaMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

// Reemplaza al antiguo RabbitMQConfig: ms-recargas publica en el tópico
// atuncar_queue que consume ms-riesgo.
// ProducerFactory/KafkaTemplate se declaran explícitos, tipados a
// RecargaMessage: el KafkaTemplate<Object,Object> autoconfigurado por
// Spring Boot no matchea por generics con lo que pide RecargaProducer.
@Configuration
public class KafkaTopicConfig {

    public static final String ATUNCAR_TOPIC = "atuncar_queue";

    @Bean
    public NewTopic atuncarTopic() {
        return TopicBuilder.name(ATUNCAR_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, RecargaMessage> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                JsonSerializer.ADD_TYPE_INFO_HEADERS, false
        );
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, RecargaMessage> kafkaTemplate(
            ProducerFactory<String, RecargaMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
