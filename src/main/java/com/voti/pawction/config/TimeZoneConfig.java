package com.voti.pawction.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {
    @Value("${app.zone-id:America/New_York}")
    private String zoneId;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
