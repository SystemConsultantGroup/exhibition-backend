package kr.ac.skku.scg.exhibition.eventperiod.controller;

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
import kr.ac.skku.scg.exhibition.eventperiod.dto.response.EventPeriodResponse;
import kr.ac.skku.scg.exhibition.eventperiod.service.EventPeriodService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EventPeriodController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs
class EventPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventPeriodService eventPeriodService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(eventPeriodService.get(id)).thenReturn(new EventPeriodResponse(
                id, exhibitionId, "2025-2학기",
                Instant.parse("2025-09-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"),
                Instant.now(), Instant.now()));

        mockMvc.perform(get("/event-periods/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("event-periods-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("이벤트 기간 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("이벤트 기간 ID"),
                                fieldWithPath("exhibitionId").description("전시 ID"),
                                fieldWithPath("name").description("이벤트 기간명"),
                                fieldWithPath("startTime").description("시작 시각"),
                                fieldWithPath("endTime").description("종료 시각"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(eventPeriodService.list(any())).thenReturn(List.of(new EventPeriodResponse(
                UUID.randomUUID(), exhibitionId, "2025-1학기",
                Instant.parse("2025-03-01T00:00:00Z"), Instant.parse("2025-06-30T00:00:00Z"),
                Instant.now(), Instant.now())));

        mockMvc.perform(get("/event-periods").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("2025-1학기"))
                .andDo(document("event-periods-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("exhibitionId").description("전시 ID")
                        ),
                        responseFields(
                                fieldWithPath("items").description("이벤트 기간 목록"),
                                fieldWithPath("items[].id").description("이벤트 기간 ID"),
                                fieldWithPath("items[].exhibitionId").description("전시 ID"),
                                fieldWithPath("items[].name").description("이벤트 기간명"),
                                fieldWithPath("items[].startTime").description("시작 시각"),
                                fieldWithPath("items[].endTime").description("종료 시각"),
                                fieldWithPath("items[].createdAt").description("생성 일시"),
                                fieldWithPath("items[].updatedAt").description("수정 일시"),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 건수")
                        )));
    }
}
