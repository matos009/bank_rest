package com.example.bankcards.cors;

import com.example.bankcards.config.cors.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CorsTest.PingController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
@AutoConfigureMockMvc(addFilters = false)
class CorsTest {

    @Autowired MockMvc mvc;
    @RestController
    static class PingController {
        @GetMapping("/cards/me")
        public String me() { return "ok"; }
    }

    @Test
    void preflight_ok() throws Exception {
        mvc.perform(options("/cards/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Origin")));
    }
}