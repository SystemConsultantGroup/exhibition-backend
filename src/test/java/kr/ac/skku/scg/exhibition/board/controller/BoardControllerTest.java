package kr.ac.skku.scg.exhibition.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.service.BoardService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BoardController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(boardService.get(id)).thenReturn(new BoardResponse(
                id,
                UUID.randomUUID(),
                "공지",
                "안내 내용",
                List.of(UUID.randomUUID()),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(get("/boards/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("boards-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("게시판 글 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시판 글 ID"),
                                fieldWithPath("exhibitionId").description("전시 ID"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("내용"),
                                fieldWithPath("attachmentMediaIds").description("첨부 미디어 ID 목록"),
                                fieldWithPath("authorUserId").description("작성자 사용자 ID"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(boardService.list(any())).thenReturn(List.of(new BoardResponse(
                UUID.randomUUID(),
                exhibitionId,
                "공지",
                "안내 내용",
                List.of(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        )));

        mockMvc.perform(get("/boards").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("공지"))
                .andDo(document("boards-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("exhibitionId").description("전시 ID")
                        ),
                        responseFields(
                                fieldWithPath("items").description("게시판 목록"),
                                fieldWithPath("items[].id").description("게시판 글 ID"),
                                fieldWithPath("items[].exhibitionId").description("전시 ID"),
                                fieldWithPath("items[].title").description("제목"),
                                fieldWithPath("items[].content").description("내용"),
                                fieldWithPath("items[].attachmentMediaIds").description("첨부 미디어 ID 목록"),
                                fieldWithPath("items[].authorUserId").description("작성자 사용자 ID"),
                                fieldWithPath("items[].createdAt").description("생성 일시"),
                                fieldWithPath("items[].updatedAt").description("수정 일시"),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 건수")
                        )));
    }
}
