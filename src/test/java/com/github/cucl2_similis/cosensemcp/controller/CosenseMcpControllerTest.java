package com.github.cucl2_similis.cosensemcp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.cucl2_similis.cosensemcp.service.CosenseApiException;
import com.github.cucl2_similis.cosensemcp.service.CosenseApiService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * documents/test-spec.md の MP-01〜MP-03 と MG-01〜MG-02 に対応する.
 */
class CosenseMcpControllerTest {

    // documents/test-spec.md: MP-01
    @Test
    void searchPagesReturnsTitlesFromService() {
        CosenseApiService service = mock(CosenseApiService.class);
        when(service.searchPages("spring")).thenReturn(List.of("Spring Boot CORS", "Spring Security"));

        CosenseMcpController controller = new CosenseMcpController(service);

        List<String> actual = controller.searchPages("spring");

        assertThat(actual).containsExactly("Spring Boot CORS", "Spring Security");
    }

    // documents/test-spec.md: MP-02
    @Test
    void searchPagesReturnsEmptyListWhenServiceReturnsNoMatches() {
        CosenseApiService service = mock(CosenseApiService.class);
        when(service.searchPages("nomatch")).thenReturn(List.of());

        CosenseMcpController controller = new CosenseMcpController(service);

        List<String> actual = controller.searchPages("nomatch");

        assertThat(actual).isEmpty();
    }

    // documents/test-spec.md: MP-03
    @Test
    void searchPagesPropagatesCosenseApiException() {
        CosenseApiService service = mock(CosenseApiService.class);
        when(service.searchPages("secret"))
                .thenThrow(new CosenseApiException("認証エラー: connect.sidが無効か期限切れです"));

        CosenseMcpController controller = new CosenseMcpController(service);

        assertThatThrownBy(() -> controller.searchPages("secret"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("認証エラー: connect.sidが無効か期限切れです");
    }

    // documents/test-spec.md: MG-01
    @Test
    void getPageReturnsContentFromService() {
        CosenseApiService service = mock(CosenseApiService.class);
        when(service.getPageContent("Spring Boot")).thenReturn("1行目\n2行目");

        CosenseMcpController controller = new CosenseMcpController(service);

        String actual = controller.getPage("Spring Boot");

        assertThat(actual).isEqualTo("1行目\n2行目");
    }

    // documents/test-spec.md: MG-02
    @Test
    void getPagePropagatesCosenseApiException() {
        CosenseApiService service = mock(CosenseApiService.class);
        when(service.getPageContent("missing"))
                .thenThrow(new CosenseApiException("指定したページが見つかりません"));

        CosenseMcpController controller = new CosenseMcpController(service);

        assertThatThrownBy(() -> controller.getPage("missing"))
                .isInstanceOf(CosenseApiException.class)
                .hasMessage("指定したページが見つかりません");
    }
}
