package com.github.cucl2_similis.cosensemcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.cucl2_similis.cosensemcp.config.CosenseProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * documents/test-spec.md の SP-01〜SP-05 と GP-01〜GP-06 に対応する。
 */
class CosenseApiServiceTest {

    private static final String PROJECT_NAME = "private-project";
    private static final String CONNECT_SID = "s%3Aexample";

    private MockRestServiceServer server;

    private CosenseApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.service = new CosenseApiService(builder, new CosenseProperties(PROJECT_NAME, CONNECT_SID, 0));
    }

    // documents/test-spec.md: SP-01
    @Test
    void searchPagesReturnsTitlesInOrder() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=spring"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pages": [
                            { "title": "Spring Boot CORS" },
                            { "title": "Spring Security" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<String> actual = this.service.searchPages("spring");

        assertThat(actual).containsExactly("Spring Boot CORS", "Spring Security");
    }

    // documents/test-spec.md: SP-02
    @Test
    void searchPagesReturnsEmptyListWhenApiReturnsNoMatches() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=nomatch"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pages": []
                        }
                        """, MediaType.APPLICATION_JSON));

        List<String> actual = this.service.searchPages("nomatch");

        assertThat(actual).isEmpty();
    }

    // documents/test-spec.md: SP-03
    @Test
    void searchPagesMaps401ToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=auth"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> this.service.searchPages("auth"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("認証エラー: connect.sidが無効か期限切れです");
    }

    // documents/test-spec.md: SP-04
    @Test
    void searchPagesMapsOther4xxToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=bad"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> this.service.searchPages("bad"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("リクエストエラーが発生しました (HTTP 400)");
    }

    // documents/test-spec.md: SP-05
    @Test
    void searchPagesMaps5xxToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=server"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> this.service.searchPages("server"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("Cosenseサーバーエラーが発生しました (HTTP 500)");
    }

    // documents/test-spec.md: GP-01
    @Test
    void getPageReturnsAllLinesJoinedByLineFeed() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/Spring%20Boot"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "title": "Spring Boot",
                          "lines": [
                            { "text": "1行目" },
                            { "text": "2行目" },
                            { "text": "3行目" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String actual = this.service.getPageContent("Spring Boot");

        assertThat(actual).isEqualTo("1行目\n2行目\n3行目");
    }

    // documents/test-spec.md: GP-02
    @Test
    void getPageReturnsSingleLineWithoutExtraLineFeed() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/SingleLine"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "title": "SingleLine",
                          "lines": [
                            { "text": "単一行" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String actual = this.service.getPageContent("SingleLine");

        assertThat(actual).isEqualTo("単一行");
    }

    // documents/test-spec.md: GP-03
    @Test
    void getPageMaps404ToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/missing"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> this.service.getPageContent("missing"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("指定したページが見つかりません");
    }

    // documents/test-spec.md: GP-04
    @Test
    void getPageMaps401ToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/secret"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> this.service.getPageContent("secret"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("認証エラー: connect.sidが無効か期限切れです");
    }

    // documents/test-spec.md: GP-05
    @Test
    void getPageMapsOther4xxToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/invalid"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> this.service.getPageContent("invalid"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("リクエストエラーが発生しました (HTTP 400)");
    }

    // documents/test-spec.md: GP-06
    @Test
    void getPageMaps5xxToReadableMessage() {
        this.server.expect(requestTo("https://scrapbox.io/api/pages/private-project/failure"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> this.service.getPageContent("failure"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("Cosenseサーバーエラーが発生しました (HTTP 502)");
    }
}
