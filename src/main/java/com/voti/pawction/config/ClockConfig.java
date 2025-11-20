package com.voti.pawction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {
    @Bean
    public Clock appClock(@Value("${app.zone-id:America/New_York}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
