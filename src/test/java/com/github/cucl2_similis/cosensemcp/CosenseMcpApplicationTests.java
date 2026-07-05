package com.github.cucl2_similis.cosensemcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.cucl2_similis.cosensemcp.controller.CosenseMcpController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * documents/test-spec.md の CT-01 と CT-02 に対応する。
 */
@SpringBootTest(properties = {
        "cosense.project-name=test-project",
        "cosense.connect-sid=s%3Atest"
})
class CosenseMcpApplicationTests {

    @Autowired
    private CosenseMcpController controller;

    // documents/test-spec.md: CT-01
    @Test
    void contextLoads() {
    }

    // documents/test-spec.md: CT-02
    @Test
    void controllerBeanIsAvailable() {
        assertThat(this.controller).isNotNull();
    }

}
