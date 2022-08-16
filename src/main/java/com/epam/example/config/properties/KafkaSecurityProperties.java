package com.epam.example.config.properties;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class KafkaSecurityProperties {

  @NotEmpty
  private String securityProtocol = "SASL_SSL";
  @NotEmpty
  private String saslMechanism = "SCRAM-SHA-512";
  @NotEmpty
  private String saslJaasConfigStringTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
  private String username;
  private String password;
  @Valid
  @NestedConfigurationProperty
  private KafkaTrustStoreProperties trustStore;

  public boolean isEmpty() {
    return StringUtils.isAllBlank(username, password)
        && (trustStore == null || trustStore.isEmpty());
  }
}
