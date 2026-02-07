package kr.ac.skku.scg.exhibition.item.controller;

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

import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ItemController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        when(itemService.get(id)).thenReturn(new ItemResponse(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Smart Campus",
                "desc",
                "홍길동,양현준",
                "김교수",
                null,
                null,
                null));

        mockMvc.perform(get("/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("items-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("아이템 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("아이템 ID"),
                                fieldWithPath("exhibitionId").description("전시 ID"),
                                fieldWithPath("categoryId").description("카테고리 ID"),
                                fieldWithPath("eventPeriodId").description("이벤트 기간 ID").optional(),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("description").description("설명").optional(),
                                fieldWithPath("participantNames").description("참여자 명단: 쉼표로 구분됨").optional(),
                                fieldWithPath("advisorNames").description("지도교수 명단: 쉼표로 구분됨").optional(),
                                fieldWithPath("thumbnailMediaId").description("썸네일 미디어 ID").optional(),
                                fieldWithPath("posterMediaId").description("포스터 미디어 ID").optional(),
                                fieldWithPath("presentationVideoMediaId").description("발표 영상 미디어 ID").optional()
                        )));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(itemService.list(any())).thenReturn(List.of(new ItemResponse(
                UUID.randomUUID(), exhibitionId, UUID.randomUUID(), null,
                "Smart Campus", "desc",
                "홍길동", "김교수",
                null, null, null)));

        mockMvc.perform(get("/items").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Smart Campus"))
                .andDo(document("items-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("exhibitionId").description("전시 ID"),
                                parameterWithName("categoryId").optional().description("카테고리 ID"),
                                parameterWithName("eventPeriodId").optional().description("이벤트 기간 ID")
                        ),
                        responseFields(
                                fieldWithPath("items").description("아이템 목록"),
                                fieldWithPath("items[].id").description("아이템 ID"),
                                fieldWithPath("items[].exhibitionId").description("전시 ID"),
                                fieldWithPath("items[].categoryId").description("카테고리 ID"),
                                fieldWithPath("items[].eventPeriodId").description("이벤트 기간 ID").optional(),
                                fieldWithPath("items[].title").description("제목"),
                                fieldWithPath("items[].description").description("설명").optional(),
                                fieldWithPath("items[].participantNames").description("참여자명단: 쉼표로 구분됨").optional(),
                                fieldWithPath("items[].advisorNames").description("지도교수명단: 쉼표로 구분됨").optional(),
                                fieldWithPath("items[].thumbnailMediaId").description("썸네일 미디어 ID").optional(),
                                fieldWithPath("items[].posterMediaId").description("포스터 미디어 ID").optional(),
                                fieldWithPath("items[].presentationVideoMediaId").description("발표 영상 미디어 ID").optional(),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 건수")
                        )));
    }
}
