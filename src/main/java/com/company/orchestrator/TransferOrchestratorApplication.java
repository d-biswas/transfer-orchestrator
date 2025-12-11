package com.company.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TransferOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferOrchestratorApplication.class, args);
    }

}
