package com.github.cucl2_similis.cosensemcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cosense 接続に必要な環境変数を、1つの設定オブジェクトとして扱う.
 */
@ConfigurationProperties(prefix = "cosense")
public record CosenseProperties(String projectName, String connectSid, long apiWaitMs) {
}
