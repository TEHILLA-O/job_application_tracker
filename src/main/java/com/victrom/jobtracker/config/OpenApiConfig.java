package com.victrom.jobtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Job Application Tracker API")
                .version("1.1.0")
                .description("I built this to keep my own job search in one place: applications, pipeline rules, timeline notes and analytics.")
                .contact(new Contact().name("Tehilla").url("https://github.com/TEHILLA-O"))
                .license(new License().name("MIT")));
    }
}
