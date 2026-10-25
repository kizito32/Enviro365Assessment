package com.enviro.assessment.junior.kizitokayizzi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    // A Clock bean (instead of LocalDate.now()) lets tests control the date
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
