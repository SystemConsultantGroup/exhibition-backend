package kr.ac.skku.scg.exhibition.media.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.MinioProperties;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.dto.response.MediaFileResponse;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MediaController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    private final List<InputStream> openedStreams = new ArrayList<>();

    /** 정상 응답과 Range 응답 모두 스트림을 닫고 전체 버퍼링을 하지 않는다. */
    @AfterEach
    void verifyStreamsClosed() throws Exception {
        for (InputStream stream : openedStreams) {
            verify(stream).close();
            verify(stream, never()).readAllBytes();
        }
    }

    @Test
    void getFileById() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "image/jpeg");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Length", "3"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
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

    /** 기존 영상 재생에 필요한 부분 다운로드 응답을 유지한다. */
    @Test
    void getFileRange() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "video/mp4");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(get("/media/{id}", id).header("Range", "bytes=1-2"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Type", "video/mp4"))
                .andExpect(header().string("Content-Range", "bytes 1-2/3"))
                .andExpect(header().string("Content-Length", "2"))
                .andExpect(content().bytes(new byte[]{2, 3}));
    }

    /** 파일 끝을 기준으로 한 부분 요청도 기존대로 처리한다. */
    @Test
    void getFileSuffixRange() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "video/mp4");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(get("/media/{id}", id).header("Range", "bytes=-1"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-2/3"))
                .andExpect(content().bytes(new byte[]{3}));
    }

    /** 역순의 다중 범위도 스트림을 다시 열어 기존 multipart 응답을 유지한다. */
    @Test
    void getFileMultipleRanges() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "video/mp4");
        when(mediaService.getFile(id)).thenReturn(file);

        var result = mockMvc.perform(get("/media/{id}", id).header("Range", "bytes=2-2,0-0"))
                .andExpect(status().isPartialContent())
                .andExpect(content().contentTypeCompatibleWith("multipart/byteranges"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("Content-Range: bytes 2-2/3\r\n\r\n\u0003",
                        "Content-Range: bytes 0-0/3\r\n\r\n\u0001");
    }

    /** 유효하지 않은 범위는 기존처럼 416으로 응답한다. */
    @Test
    void getFileUnsatisfiableRange() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "video/mp4");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(get("/media/{id}", id).header("Range", "bytes=3-"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */3"));
    }

    /** HEAD 요청은 기존 파일 크기와 타입을 그대로 반환한다. */
    @Test
    void headFile() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "image/jpeg");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(head("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Length", "3"));
    }

    /** 존재하지 않는 미디어의 오류 계약을 유지한다. */
    @Test
    void getMissingFile() throws Exception {
        UUID id = UUID.randomUUID();
        when(mediaService.getFile(id)).thenThrow(new NotFoundException("Media not found: " + id));

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Media not found: " + id));
    }

    @Test
    void getFileById_invalidMimeTypeFallback() throws Exception {
        UUID id = UUID.randomUUID();
        MediaFileResponse file = file(id, "invalid mime");
        when(mediaService.getFile(id)).thenReturn(file);

        mockMvc.perform(get("/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }

    /** 실제 서비스의 지연 로딩 Resource를 사용해 HTTP 계약과 스트림 수명을 함께 검증한다. */
    private MediaFileResponse file(UUID id, String contentType) throws Exception {
        MediaAssetRepository repository = mock(MediaAssetRepository.class);
        MinioClient minio = mock(MinioClient.class);
        MinioProperties properties = new MinioProperties();
        properties.setBucket("test");
        when(repository.findById(id)).thenReturn(Optional.of(
                new MediaAssetEntity(id, null, null, "slug/a.bin", contentType, 3L)));
        StatObjectResponse metadata = mock(StatObjectResponse.class);
        when(metadata.size()).thenReturn(3L);
        when(metadata.contentType()).thenReturn(contentType);
        when(minio.statObject(any(StatObjectArgs.class))).thenReturn(metadata);
        when(minio.getObject(any(GetObjectArgs.class))).thenAnswer(call -> {
            InputStream input = spy(new ByteArrayInputStream(new byte[]{1, 2, 3}));
            openedStreams.add(input);
            return new GetObjectResponse(new Headers.Builder().build(), "test", null, "slug/a.bin", input);
        });

        MediaFileResponse file = new MediaService(repository, minio, properties).getFile(id);
        assertThat(file.resource().contentLength()).isEqualTo(3L);
        verify(minio, never()).getObject(any(GetObjectArgs.class));
        return file;
    }
}
