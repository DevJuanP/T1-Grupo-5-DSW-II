package com.paygo.riesgo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Reemplaza al antiguo RabbitMQConfig: mismo nombre de tópico que
// ms-recargas para que RiesgoConsumer escuche atuncar_queue.
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
}
