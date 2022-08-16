package com.epam.example.config.properties;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class KafkaTrustStoreProperties {

  private String path;
  private String password;

  public boolean isEmpty() {
    return StringUtils.isAllBlank(path, password);
  }
}
