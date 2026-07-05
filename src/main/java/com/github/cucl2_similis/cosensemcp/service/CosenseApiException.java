package com.github.cucl2_similis.cosensemcp.service;

/**
 * Cosense API 向けの失敗を、MCP ツール側で扱いやすい業務例外へ寄せる.
 */
public class CosenseApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CosenseApiException(String message) {
        super(message);
    }
}
