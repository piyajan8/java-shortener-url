package com.macode101.shortenerurl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application")
public class ApplicationConfiguration {

    private JWT jwt = new JWT();
    private String baseUrl;

    @Getter
    @Setter
    public static class JWT {
        private String secret;
        private Long expiration;
    }
}
