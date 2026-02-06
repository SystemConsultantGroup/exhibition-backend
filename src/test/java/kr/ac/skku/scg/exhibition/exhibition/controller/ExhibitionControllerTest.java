package kr.ac.skku.scg.exhibition.exhibition.controller;

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
        when(exhibitionService.get(id)).thenReturn(new ExhibitionResponse(
                id,
                "cse-2026",
                "CSE 2026",
                "설명",
                null,
                false,
                null,
                null,
                null,
                null,
                null
        ));

        mockMvc.perform(get("/exhibitions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("exhibitions-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("전시 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("전시 ID"),
                                fieldWithPath("slug").description("전시 slug"),
                                fieldWithPath("name").description("전시명"),
                                fieldWithPath("description").description("전시 설명").optional(),
                                fieldWithPath("logoMediaId").description("로고 미디어 ID").optional(),
                                fieldWithPath("popupEnabled").description("팝업 사용 여부"),
                                fieldWithPath("popupImageMediaId").description("팝업 이미지 미디어 ID").optional(),
                                fieldWithPath("popupUrl").description("팝업 링크 URL").optional(),
                                fieldWithPath("introTitle").description("인트로 제목").optional(),
                                fieldWithPath("introDescription").description("인트로 설명").optional(),
                                fieldWithPath("introVideoMediaId").description("인트로 비디오 미디어 ID").optional()
                        )));
    }

    @Test
    void list() throws Exception {
        when(exhibitionService.list(any())).thenReturn(List.of(
                new ExhibitionResponse(
                        UUID.randomUUID(),
                        "cse-2026",
                        "CSE 2026",
                        "설명",
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        ));

        mockMvc.perform(get("/exhibitions").param("q", "cse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("CSE 2026"))
                .andDo(document("exhibitions-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("q").optional().description("전시명 검색어")
                        ),
                        responseFields(
                                fieldWithPath("items").description("전시 목록"),
                                fieldWithPath("items[].id").description("전시 ID"),
                                fieldWithPath("items[].slug").description("전시 slug"),
                                fieldWithPath("items[].name").description("전시명"),
                                fieldWithPath("items[].description").description("전시 설명").optional(),
                                fieldWithPath("items[].logoMediaId").description("로고 미디어 ID").optional(),
                                fieldWithPath("items[].popupEnabled").description("팝업 사용 여부"),
                                fieldWithPath("items[].popupImageMediaId").description("팝업 이미지 미디어 ID").optional(),
                                fieldWithPath("items[].popupUrl").description("팝업 링크 URL").optional(),
                                fieldWithPath("items[].introTitle").description("인트로 제목").optional(),
                                fieldWithPath("items[].introDescription").description("인트로 설명").optional(),
                                fieldWithPath("items[].introVideoMediaId").description("인트로 비디오 미디어 ID").optional(),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 건수")
                        )));
    }
}
