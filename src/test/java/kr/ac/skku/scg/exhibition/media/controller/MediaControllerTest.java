package kr.ac.skku.scg.exhibition.media.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaResponse;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MediaController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.getMetadata(id)).thenReturn(new MediaResponse(
                id, UUID.randomUUID(), UUID.randomUUID(), "a.jpg", "image/jpeg", 123L, Instant.now()));

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("media-get", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(mediaService.list(any())).thenReturn(List.of(
                new MediaResponse(UUID.randomUUID(), exhibitionId, null, "a.jpg", "image/jpeg", 123L, Instant.now())
        ));

        mockMvc.perform(get("/media").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].objectKey").value("a.jpg"))
                .andDo(document("media-list", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    @Test
    void getFile() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.getFile(id)).thenReturn(new MediaFileResponse("a.jpg", "image/jpeg", 3L, new byte[]{1, 2, 3}));

        mockMvc.perform(get("/media/{id}/file", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("a.jpg")))
                .andDo(document("media-get-file", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }
}
