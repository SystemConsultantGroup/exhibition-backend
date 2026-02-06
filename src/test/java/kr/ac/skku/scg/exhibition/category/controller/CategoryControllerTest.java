package kr.ac.skku.scg.exhibition.category.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CategoryController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(categoryService.get(id)).thenReturn(new CategoryResponse(id, exhibitionId, "Web", Instant.now(), Instant.now()));

        mockMvc.perform(get("/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("categories-get"));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(categoryService.list(any())).thenReturn(List.of(
                new CategoryResponse(UUID.randomUUID(), exhibitionId, "AI", Instant.now(), Instant.now())
        ));

        mockMvc.perform(get("/categories").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("AI"))
                .andDo(document("categories-list"));
    }
}
