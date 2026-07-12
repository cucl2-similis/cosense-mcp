package com.github.cucl2_similis.cosensemcp.controller;

import com.github.cucl2_similis.cosensemcp.service.CosenseApiService;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Controller;

/**
 * Cosense 向け MCP ツールを公開するコントローラ.
 */
@Controller
public class CosenseMcpController {

    private final CosenseApiService cosenseApiService;

    /**
     * コントローラを初期化する.
     *
     * @param cosenseApiService Cosense API 呼び出しを担うサービス
     */
    public CosenseMcpController(CosenseApiService cosenseApiService) {
        this.cosenseApiService = cosenseApiService;
    }

    /**
     * キーワードに一致したページタイトル一覧を返す.
     *
     * <p>検索対象はタイトルだけでなく本文も含み、<br>
     * MCP クライアントが候補ページを絞り込むための入力として使う。
     *
     * @param query 検索キーワード
     * @return マッチしたページタイトル一覧
     */
    @McpTool(name = "cosense_search_pages",
             description = """
                           指定されたキーワードで Cosense ページを検索します。
                           タイトルだけでなく本文にもマッチしたページを対象とし、該当ページのタイトル一覧を返します。
                           """)
    public List<String> searchPages(@McpToolParam(description = "検索キーワード") String query) {
        return this.cosenseApiService.searchPages(query);
    }

    /**
     * 指定タイトルのページ本文を返す.
     *
     * <p>本文は Cosense API から取得した全量テキストをそのまま返し、<br>
     * MCP クライアントが詳細内容を参照できるようにする。
     *
     * @param title 取得対象のページタイトル
     * @return ページ本文
     */
    @McpTool(name = "cosense_get_page",
             description = """
                           指定されたタイトルの Cosense ページ本文を取得します。
                           本文は省略せず全量を返し、ページの詳細内容をそのまま参照できます。
                           """)
    public String getPage(@McpToolParam(description = "ページタイトル") String title) {
        return this.cosenseApiService.getPageContent(title);
    }
}
