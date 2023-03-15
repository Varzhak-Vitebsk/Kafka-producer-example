package com.epam.example.service;

import com.epam.example.OrderKafkaAvroMessage;
import com.epam.example.config.properties.KafkaProducerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;

import static com.epam.example.config.KafkaConfiguration.ORDER_SENDER_PROPERTIES;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMessageSender {

  private final KafkaSender<String, OrderKafkaAvroMessage> sender;
  @Qualifier(ORDER_SENDER_PROPERTIES)
  private final KafkaProducerProperties properties;

  public Mono<Void> sendMessage(OrderKafkaAvroMessage message) {
    return sender.createOutbound().send(Mono.just(new ProducerRecord<>(
            properties.getTopic(),
            message.getOrderNumber(),
            message)))
        .then()
        .doOnSuccess(v -> log.info("Message sent successfully to topic {} for order {}",
            properties.getTopic(),
            message.getOrderNumber()))
        .doOnError(e -> log.error(
            "Something went wrong while sending a message to topic {} for order {}",
            properties.getTopic(),
            message.getOrderNumber(), e));
  }
}
