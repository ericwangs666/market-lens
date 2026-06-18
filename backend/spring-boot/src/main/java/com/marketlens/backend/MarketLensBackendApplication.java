package com.marketlens.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.marketlens.backend.mapper")
@SpringBootApplication
public class MarketLensBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketLensBackendApplication.class, args);
    }
}
