package com.voti.pawction;

import com.voti.pawction.config.AuctionSchedulerProperties;
import com.voti.pawction.services.user.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties(AuctionSchedulerProperties.class)
public class PawctionApplication {

    public static void main(String[] args) {
        //SpringApplication.run(PawctionApplication.class, args);

        ApplicationContext context = SpringApplication.run(PawctionApplication.class, args);

        var userRepository = context.getBean(UserService.class);
        //userRepository.createTestUser();
        //userRepository.createTestBid();

        //create an auction
        //userRepository.overallProcess();

        //create users to test bidding
        //userRepository.createAuction();
        //userRepository.placeBidAsQualifiedUser();


    }

}
