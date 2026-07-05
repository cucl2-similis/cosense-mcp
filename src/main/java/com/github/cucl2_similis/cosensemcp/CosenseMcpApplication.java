package com.github.cucl2_similis.cosensemcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CosenseMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CosenseMcpApplication.class, args);
    }

}
