package com.voti.pawction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "auction.scheduler")
public class AuctionSchedulerProperties {
    private boolean enabled = true;
    private String cron = "*/30 * * * * *";
    private int graceSeconds = 2;
}
