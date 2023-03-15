package com.epam.example.config.properties;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class KafkaConsumerProperties {

  @NotEmpty
  private List<String> bootstrapServers;
  @NotEmpty
	private String topic;
  @NotEmpty
	private String consumerGroupId;
  @NestedConfigurationProperty
  @Valid
  @NotNull
  private KafkaSecurityProperties security;
}
