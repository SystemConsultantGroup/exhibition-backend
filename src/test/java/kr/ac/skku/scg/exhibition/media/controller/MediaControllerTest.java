package kr.ac.skku.scg.exhibition.media.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
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
    void getFileById() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.getFile(id)).thenReturn(new MediaFileResponse("a.jpg", "image/jpeg", 3L, new byte[]{1, 2, 3}));

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andDo(document("media-get-file",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미디어 ID")
                        ),
                        responseHeaders(
                                headerWithName("Content-Type").description("미디어 MIME 타입"),
                                headerWithName("Content-Length").description("파일 바이트 크기")
                        )));
    }

    @Test
    void getFileById_invalidMimeTypeFallback() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.getFile(id)).thenReturn(new MediaFileResponse("a.bin", "invalid mime", 3L, new byte[]{1, 2, 3}));

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }
}
