package kr.ac.skku.scg.exhibition.exhibition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExhibitionController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class ExhibitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExhibitionService exhibitionService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(exhibitionService.get(id)).thenReturn(new ExhibitionResponse(id, "cse-2026", "CSE 2026", Instant.now(), Instant.now()));

        mockMvc.perform(get("/exhibitions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("exhibitions-get"));
    }

    @Test
    void list() throws Exception {
        when(exhibitionService.list(any())).thenReturn(List.of(
                new ExhibitionResponse(UUID.randomUUID(), "cse-2026", "CSE 2026", Instant.now(), Instant.now())
        ));

        mockMvc.perform(get("/exhibitions").param("q", "cse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CSE 2026"))
                .andDo(document("exhibitions-list"));
    }
}
