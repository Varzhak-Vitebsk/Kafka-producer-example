package com.epam.example.config.properties;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Data
@Validated
public class KafkaProducerProperties {

    @NotEmpty
    private List<String> bootstrapServers;
    @NotBlank
    private String topic;
    private Duration deliveryTimeout;
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private KafkaSecurityProperties security;
}
