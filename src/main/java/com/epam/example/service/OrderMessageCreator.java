package com.epam.example.service;

import com.epam.example.OrderKafkaAvroMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderMessageCreator {

  private final OrderMessageSender messageSender;

  @Scheduled(fixedRate = 2000, initialDelay = 1000)
  public void createMessage() {
    log.info("Sending");
    Mono.just(OrderKafkaAvroMessage.newBuilder()
            .setOrderNumber(UUID.randomUUID().toString())
            .setComment("This is a comment.")
            .build())
        .flatMap(messageSender::sendMessage)
        .subscribe();
  }

}
