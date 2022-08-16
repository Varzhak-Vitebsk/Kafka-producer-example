package com.epam.example.config;

import com.epam.example.config.properties.KafkaProducerProperties;
import com.epam.example.config.properties.KafkaSecurityProperties;
import com.epam.example.config.properties.KafkaTrustStoreProperties;
import com.epam.example.model.OrderKafkaMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class KafkaConfiguration {

  public static final String ORDER_SENDER_PROPERTIES = "orderKafkaSenderProperties";
  public static final String KAFKA_OBJECT_MAPPER = "kafkaObjectMapper";

  @Bean(KAFKA_OBJECT_MAPPER)
  public ObjectMapper kafkaObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    return objectMapper;
  }

  @Bean(ORDER_SENDER_PROPERTIES)
  @ConfigurationProperties(value = "kafka.producer.order")
  public KafkaProducerProperties orderKafkaSenderProperties() {
    return new KafkaProducerProperties();
  }

  @Bean
  public KafkaSender<String, OrderKafkaMessage> orderKafkaSender(
      @Qualifier(ORDER_SENDER_PROPERTIES) KafkaProducerProperties producerProperties,
      @Qualifier(KAFKA_OBJECT_MAPPER) ObjectMapper mapper) {
    SenderOptions<String, OrderKafkaMessage> senderOptions = SenderOptions.<String, OrderKafkaMessage>create(
            buildProducerConfigs(producerProperties)).maxInFlight(1024)
        .withKeySerializer(new StringSerializer())
        .withValueSerializer(new JsonSerializer<>(mapper));

    return KafkaSender.create(senderOptions);
  }

  private Map<String, Object> buildProducerConfigs(KafkaProducerProperties producerProperties) {

    Map<String, Object> clusterConfigs = new HashMap<>();
    clusterConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProperties.getBootstrapServers());
    Optional.ofNullable(producerProperties.getDeliveryTimeout())
        .map(deliverTimeout -> (int) deliverTimeout.toMillis())
        .ifPresent(deliveryTimeoutMillis ->
            clusterConfigs.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMillis)
        );

    return Stream.of(clusterConfigs, buildSecurityConfigs(producerProperties.getSecurity()))
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> buildSecurityConfigs(KafkaSecurityProperties securityProperties) {
    if (Objects.isNull(securityProperties) || securityProperties.isEmpty()) {
      return Map.of();
    }

    final var jaasConfig = String.format(
        securityProperties.getSaslJaasConfigStringTemplate(),
        securityProperties.getUsername(),
        securityProperties.getPassword()
    );

    final var securityConfig = Map.of(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProperties.getSecurityProtocol(),
        SaslConfigs.SASL_MECHANISM, securityProperties.getSaslMechanism(),
        SaslConfigs.SASL_JAAS_CONFIG, jaasConfig
    );

    return Stream.of(securityConfig, buildTrustStoreConfigs(securityProperties.getTrustStore()))
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> buildTrustStoreConfigs(KafkaTrustStoreProperties trustStoreProperties) {
    if (Objects.isNull(trustStoreProperties) || trustStoreProperties.isEmpty()) {
      return Map.of();
    }
    return Map.of(
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreProperties.getPath(),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStoreProperties.getPassword()
    );
  }
}
