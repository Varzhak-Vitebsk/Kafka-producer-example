package com.epam.example.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderKafkaMessage {

  String orderNumber;
  String comment;
}
