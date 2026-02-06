package kr.ac.skku.scg.exhibition.classification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.classification.dto.response.ClassificationResponse;
import kr.ac.skku.scg.exhibition.classification.service.ClassificationService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClassificationController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class ClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassificationService classificationService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(classificationService.get(id)).thenReturn(new ClassificationResponse(id, exhibitionId, "논문", Instant.now()));

        mockMvc.perform(get("/classifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("classifications-get"));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(classificationService.list(any())).thenReturn(List.of(
                new ClassificationResponse(UUID.randomUUID(), exhibitionId, "작품", Instant.now())
        ));

        mockMvc.perform(get("/classifications").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("작품"))
                .andDo(document("classifications-list"));
    }
}
