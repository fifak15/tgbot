package ru.softwarecom.telegram.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "registration")
public class RegistrationConfig {
    private String url;
}
