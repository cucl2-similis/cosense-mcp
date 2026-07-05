package com.github.cucl2_similis.cosensemcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.cucl2_similis.cosensemcp.config.CosenseProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * documents/test-spec.md の WT-01 と WT-02 に対応する。
 */
class CosenseApiServiceWaitTest {

    // documents/test-spec.md: WT-01
    @Test
    void skipsWaitWhenApiWaitMsIsZero() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CosenseApiService service = new CosenseApiService(builder, new CosenseProperties("private-project", "s%3Aexample", 0));
        server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=fast"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pages": []
                        }
                        """, MediaType.APPLICATION_JSON));

        Instant startedAt = Instant.now();
        service.searchPages("fast");
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();

        assertThat(elapsedMillis).isLessThan(200);
    }

    // documents/test-spec.md: WT-02
    @Test
    void waitsBeforeApiCallWhenApiWaitMsUsesDefaultValue() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CosenseApiService service = new CosenseApiService(builder, new CosenseProperties("private-project", "s%3Aexample", 500));
        server.expect(requestTo("https://scrapbox.io/api/pages/private-project/search/query?q=slow"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pages": []
                        }
                        """, MediaType.APPLICATION_JSON));

        Instant startedAt = Instant.now();
        service.searchPages("slow");
        long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(450);
    }
}
