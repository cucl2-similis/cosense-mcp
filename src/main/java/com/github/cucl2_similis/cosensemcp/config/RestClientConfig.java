package com.github.cucl2_similis.cosensemcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient の生成方法を設定クラスへ集約し、利用側が生成処理を持たないようにする.
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
