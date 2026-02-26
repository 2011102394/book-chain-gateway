package com.arsc.bookchaingateway;

import com.arsc.bookchaingateway.trace.config.FabricProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FabricProperties.class)
public class BookChainGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookChainGatewayApplication.class, args);
    }

}
