package com.voti.pawction.scheduling;

import com.voti.pawction.config.AuctionSchedulerProperties;
import com.voti.pawction.services.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionService auctionService;
    private final Clock clock;
    private final AuctionSchedulerProperties props;

    @Scheduled(cron = "*/30 * * * * *", zone = "${app.zone-id:America/New_York}")
    @SchedulerLock(name = "auction.closeExpired")
    public void closeExpired() {
        if (!props.isEnabled()) return;

        var cutoff = LocalDateTime.now(clock).minusSeconds(Math.max(0, props.getGraceSeconds()));
        try {
            int closed = auctionService.closeExpiredAuctions();
            if (closed > 0) {
                log.info("[auction-scheduler] closed {} auctions (cutoff {}, grace {}s)",
                        closed, cutoff, props.getGraceSeconds());
            }
        } catch (Exception e) {
            log.error("[auction-scheduler] error (cutoff {}, grace {}s)", cutoff, props.getGraceSeconds(), e);
        }
    }
}