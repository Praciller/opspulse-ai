package com.opspulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OpsPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsPulseApplication.class, args);
    }
}
