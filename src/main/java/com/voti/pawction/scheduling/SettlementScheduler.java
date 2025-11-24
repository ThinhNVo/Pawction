package com.voti.pawction.scheduling;

import com.voti.pawction.services.auction.impl.SettlementServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {
    private final SettlementServiceInterface settlementService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${auction.scheduler.settlement-delay-ms:60000}")
    public void expireOverdueSettlementsJob() {
        int processed = settlementService.expireOverdueSettlements();

        if (processed > 0) {
            System.out.println("Expired " + processed + " overdue settlements");
        }
    }
}
