package com.epam.example.config;

import com.epam.example.OrderKafkaAvroMessage;
import com.epam.example.config.properties.KafkaConsumerProperties;
import com.epam.example.config.properties.KafkaProducerProperties;
import com.epam.example.config.properties.KafkaSecurityProperties;
import com.epam.example.config.properties.KafkaTrustStoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

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
  public KafkaSender<String, OrderKafkaAvroMessage> orderKafkaSender(
      @Qualifier(ORDER_SENDER_PROPERTIES) KafkaProducerProperties producerProperties,
      @Qualifier(KAFKA_OBJECT_MAPPER) ObjectMapper mapper) {
    SenderOptions<String, OrderKafkaAvroMessage> senderOptions = SenderOptions.<String, OrderKafkaAvroMessage>create(
            buildProducerConfigs(producerProperties)).maxInFlight(1024)
        .withKeySerializer(new StringSerializer());

    return KafkaSender.create(senderOptions);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, OrderKafkaAvroMessage> orderListenerContainerFactory(KafkaConsumerProperties deliveryReadinessConsumerProperties) {
    return createKafkaListenerContainerFactory(OrderKafkaAvroMessage.class, deliveryReadinessConsumerProperties);
  }
  private <T> ConcurrentKafkaListenerContainerFactory<String, T> createKafkaListenerContainerFactory(Class<T> messageType,
      KafkaConsumerProperties consumerProperties) {
    var containerFactory = new ConcurrentKafkaListenerContainerFactory<String, T>();

    containerFactory.setConsumerFactory(
        new DefaultKafkaConsumerFactory<>(
            buildConsumerConfigs(consumerProperties)
        ));

    containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

    return containerFactory;
  }

  private Map<String, Object> buildProducerConfigs(KafkaProducerProperties producerProperties) {

    Map<String, Object> clusterConfigs = new HashMap<>();
    clusterConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProperties.getBootstrapServers());
    clusterConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    Optional.ofNullable(producerProperties.getDeliveryTimeout())
        .map(deliverTimeout -> (int) deliverTimeout.toMillis())
        .ifPresent(deliveryTimeoutMillis ->
            clusterConfigs.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMillis)
        );

    return Stream.of(clusterConfigs, buildSecurityConfigs(producerProperties.getSecurity()))
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> buildConsumerConfigs(KafkaConsumerProperties consumerProperties) {

    Map<String, Object> clusterConfigs = Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerProperties.getBootstrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getConsumerGroupId(),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class
    );
    Map<String, Object> securityConfigs = buildSecurityConfigs(consumerProperties.getSecurity());

    return Stream.of(clusterConfigs, securityConfigs)
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
