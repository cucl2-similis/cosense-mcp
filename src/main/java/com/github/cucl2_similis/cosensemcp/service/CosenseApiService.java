package com.github.cucl2_similis.cosensemcp.service;

import com.github.cucl2_similis.cosensemcp.config.CosenseProperties;
import com.github.cucl2_similis.cosensemcp.dto.PageLine;
import com.github.cucl2_similis.cosensemcp.dto.PageResponse;
import com.github.cucl2_similis.cosensemcp.dto.SearchPage;
import com.github.cucl2_similis.cosensemcp.dto.SearchResponse;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cosense API との通信を担当するサービス.
 *
 * <p>MCP ツール側では Cosense API のレスポンス構造を直接扱わず、<br>
 * タイトル一覧や本文文字列といった利用しやすい形へ変換して受け取る。<br>
 * あわせて、HTTP エラーを呼び出し側で判断しやすい例外へ統一する。
 */
@Service
public class CosenseApiService {

    private static final String BASE_URL = "https://scrapbox.io";
    private static final String SEARCH_API_PATH = "/api/pages/{project}/search/query";
    private static final String PAGE_API_PATH = "/api/pages/{project}/{title}";
    private static final String COOKIE_HEADER_VALUE_PREFIX = "connect.sid=";

    private final CosenseProperties properties;

    private final RestClient restClient;

    /**
     * サービスを初期化する.
     *
     * <p>接続先のベースURLと認証Cookieをここでまとめて設定し、<br>
     * 各メソッドが API ごとの差分だけに集中できるようにする。
     *
     * @param restClientBuilder RestClient の組み立てに使う Builder
     * @param properties Cosense 接続設定
     */
    public CosenseApiService(RestClient.Builder restClientBuilder, CosenseProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.COOKIE, COOKIE_HEADER_VALUE_PREFIX + properties.connectSid())
                .build();
    }

    /**
     * 検索APIの生レスポンスから、MCP へ返すタイトル一覧だけを抽出する.
     *
     * @param query 検索キーワード
     * @return マッチしたページタイトル一覧
     * @throws CosenseApiException Cosense API が 4xx / 5xx を返した場合
     */
    public List<String> searchPages(String query) {
        waitIfNeeded();
        SearchResponse response = this.restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_API_PATH)
                        .queryParam("q", query)
                        .build(this.properties.projectName()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .body(SearchResponse.class);

        SearchResponse searchResponse = Objects.requireNonNull(response, "Cosense API の検索レスポンスが空です");
        List<SearchPage> pages = Objects.requireNonNull(searchResponse.pages(), "Cosense API の検索結果が空です");

        return pages.stream()
                .map(SearchPage::title)
                .toList();
    }

    /**
     * ページ本文を省略せず扱うため、行配列を改行区切りの全文へ変換する.
     *
     * @param title 取得対象のページタイトル
     * @return 改行区切りで結合したページ本文
     * @throws CosenseApiException Cosense API が 4xx / 5xx を返した場合
     */
    public String getPageContent(String title) {
        waitIfNeeded();
        PageResponse response = this.restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PAGE_API_PATH)
                        .build(this.properties.projectName(), title))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .body(PageResponse.class);

        PageResponse pageResponse = Objects.requireNonNull(response, "Cosense API のページレスポンスが空です");
        List<PageLine> lines = Objects.requireNonNull(pageResponse.lines(), "Cosense API のページ本文が空です");

        return lines.stream()
                .map(PageLine::text)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 設定された待機時間がある場合だけ、API 呼び出し前に待機する.
     *
     * <p>短時間の連続アクセスで Cosense 側へ負荷を掛けすぎないようにするための防壁。
     */
    private void waitIfNeeded() {
        if (this.properties.apiWaitMs() <= 0) {
            return;
        }

        try {
            Thread.sleep(this.properties.apiWaitMs());
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cosense API 呼び出し前の待機中に割り込みが発生しました", exception);
        }
    }

    /**
     * 4xx 応答を、利用者に意味が伝わる文言へ変換する.
     *
     * @param statusCode Cosense API が返した HTTP ステータス
     * @return 呼び出し側へ返すエラーメッセージ
     */
    private String mapClientErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 401 -> "認証エラー: connect.sidが無効か期限切れです";
            case 404 -> "指定したページが見つかりません";
            default -> "リクエストエラーが発生しました (HTTP " + statusCode + ")";
        };
    }

    /**
     * 5xx 応答を、Cosense 側障害として扱うメッセージへ変換する.
     *
     * @param statusCode Cosense API が返した HTTP ステータス
     * @return 呼び出し側へ返すエラーメッセージ
     */
    private String mapServerErrorMessage(int statusCode) {
        return "Cosenseサーバーエラーが発生しました (HTTP " + statusCode + ")";
    }

    /**
     * 4xx 応答を共通例外へ変換する.
     *
     * @param request 失敗した HTTP リクエスト
     * @param response Cosense API の応答
     * @throws java.io.IOException 応答読み取り時の I/O エラー
     */
    private void handleClientError(HttpRequest request, ClientHttpResponse response) throws java.io.IOException {
        throw new CosenseApiException(mapClientErrorMessage(response.getStatusCode().value()));
    }

    /**
     * 5xx 応答を共通例外へ変換する.
     *
     * @param request 失敗した HTTP リクエスト
     * @param response Cosense API の応答
     * @throws java.io.IOException 応答読み取り時の I/O エラー
     */
    private void handleServerError(HttpRequest request, ClientHttpResponse response) throws java.io.IOException {
        throw new CosenseApiException(mapServerErrorMessage(response.getStatusCode().value()));
    }
}
