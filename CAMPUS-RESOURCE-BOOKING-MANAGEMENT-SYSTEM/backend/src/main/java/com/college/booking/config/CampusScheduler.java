package com.college.booking.config;

import com.college.booking.service.CheckInService;
import com.college.booking.service.WaitlistService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CampusScheduler {

    private final CheckInService checkInService;
    private final WaitlistService waitlistService;

    public CampusScheduler(CheckInService checkInService, WaitlistService waitlistService) {
        this.checkInService = checkInService;
        this.waitlistService = waitlistService;
    }

    @Scheduled(fixedDelay = 60000)
    public void noShowsAndWaitlist() {
        checkInService.markNoShows();
        waitlistService.expireHolds();
    }
}
