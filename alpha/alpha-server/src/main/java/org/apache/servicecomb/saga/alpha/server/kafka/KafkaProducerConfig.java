/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.server.ConfigLoading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Kafka producer configuration.
 *
 * @author Gannalyo
 * @since 2018/12/3
 */
@Configuration
//@ConfigurationProperties(prefix="spring.kafka")
@PropertySource({"classpath:kafka.properties"})
public class KafkaProducerConfig {

    @Value("${topic:default_topic}")
    private String topic;

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        return new KafkaProducer<>(ConfigLoading.loadKafkaProperties());
    }

    @Bean
    IKafkaMessageProducer kafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository) {
        return new KafkaMessageProducer(kafkaMessageRepository, topic);
    }

    @Bean
    IKafkaMessageRepository kafkaMessageRepository(KafkaMessageEntityRepository kafkaMessageEntityRepository) {
        return new KafkaMessageRepositoryImpl(kafkaMessageEntityRepository);
    }
}