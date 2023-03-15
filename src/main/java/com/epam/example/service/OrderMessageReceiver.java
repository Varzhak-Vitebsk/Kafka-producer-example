package com.epam.example.service;

import com.epam.example.OrderKafkaAvroMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderMessageReceiver {

  @KafkaListener(
      topics = "${kafka.consumer.order.topic}",
      containerFactory = "orderListenerContainerFactory"
  )
  public void consumeOrder(OrderKafkaAvroMessage message, Acknowledgment acknowledgment) {
    log.info("The message was consumed: {}", message);
    acknowledgment.acknowledge();
  }
}
