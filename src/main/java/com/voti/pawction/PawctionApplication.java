package com.voti.pawction;

import com.voti.pawction.repositories.UserRepository;
import com.voti.pawction.services.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class PawctionApplication {

    public static void main(String[] args) {
        //SpringApplication.run(PawctionApplication.class, args);

        ApplicationContext context = SpringApplication.run(PawctionApplication.class, args);

        var userRepository = context.getBean(UserService.class);
        //userRepository.createTestUser();
        //userRepository.createTestBid();
        //userRepository.createUserPool();


    }

}
