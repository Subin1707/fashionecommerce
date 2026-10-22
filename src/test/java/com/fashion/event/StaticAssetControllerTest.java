package com.fashion.event;

import com.fashion.event.controller.StaticAssetController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaticAssetControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StaticAssetController()).build();
    }

    @Test
    void existingImageIsServedAsJpeg() throws Exception {
        mockMvc.perform(get("/assets/products/seed-catalog/dresses-cream-arrow-woman-solid-cream-dress-18896.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"));
    }

    @Test
    void missingImageIsNotFound() throws Exception {
        mockMvc.perform(get("/assets/products/seed-catalog/does-not-exist.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void traversalIsNotFound() throws Exception {
        mockMvc.perform(get("/assets/{*assetPath}", "/../application.properties"))
                .andExpect(status().isNotFound());
    }

    @Test
    void headReturnsImageHeadersWithoutBody() throws Exception {
        mockMvc.perform(head("/assets/products/seed-catalog/dresses-cream-arrow-woman-solid-cream-dress-18896.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Last-Modified"));
    }
}
