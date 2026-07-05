package com.github.cucl2_similis.cosensemcp.controller;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Controller;

@Controller
public class CosenseMcpController {

    @McpTool(name = "cosense_get_page",
             description = """
                           指定されたタイトルのページ内容（本文テキスト全量）を取得します。
                           """)
    public String getPage(@McpToolParam(description = "ページタイトル") String title) {
        return "text of page " + title;
    }
}
