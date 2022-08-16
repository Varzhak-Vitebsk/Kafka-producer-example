package com.epam.example.service;

import com.epam.example.model.OrderKafkaMessage;
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
    Mono.just(OrderKafkaMessage.builder()
            .orderNumber(UUID.randomUUID().toString())
            .comment("This is a comment.")
            .build())
        .flatMap(messageSender::sendMessage)
        .subscribe();
  }

}
