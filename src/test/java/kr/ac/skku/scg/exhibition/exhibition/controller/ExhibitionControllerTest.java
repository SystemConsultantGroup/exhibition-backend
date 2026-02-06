package kr.ac.skku.scg.exhibition.exhibition.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;

@WebMvcTest(controllers = ExhibitionController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(SecurityConfig.class)
class ExhibitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExhibitionService exhibitionService;

    @Test
    void listExhibitions() throws Exception {
        ExhibitionServiceEntity entity = exhibition("cse-2026");
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        given(exhibitionService.list(true, pageable)).willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        mockMvc.perform(get("/exhibitions").param("active", "true").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("exhibitions-list",
                queryParameters(parameterWithName("active").description("활성 전시 여부")),
                responseFields(
                    fieldWithPath("items[].id").description("전시 ID"),
                    fieldWithPath("items[].slug").description("슬러그"),
                    fieldWithPath("items[].name").description("전시명"),
                    fieldWithPath("items[].description").description("설명"),
                    fieldWithPath("items[].startDate").description("시작일"),
                    fieldWithPath("items[].endDate").description("종료일"),
                    fieldWithPath("items[].isActive").description("활성 여부"),
                    fieldWithPath("items[].popupEnabled").description("팝업 활성"),
                    fieldWithPath("items[].popupImageUrl").description("팝업 이미지 URL").optional(),
                    fieldWithPath("items[].introTitle").description("소개 제목").optional(),
                    fieldWithPath("items[].introDescription").description("소개 설명").optional(),
                    fieldWithPath("items[].introVideoUrl").description("소개 영상 URL").optional(),
                    fieldWithPath("page").description("페이지"),
                    fieldWithPath("pageSize").description("페이지 크기"),
                    fieldWithPath("total").description("총 개수")
                )));
    }

    @Test
    void getExhibition() throws Exception {
        ExhibitionServiceEntity entity = exhibition("cse-2026");
        given(exhibitionService.get(entity.getId())).willReturn(entity);

        mockMvc.perform(get("/exhibitions/{id}", entity.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("exhibitions-get",
                pathParameters(parameterWithName("id").description("전시 ID"))));
    }

    private ExhibitionServiceEntity exhibition(String slug) {
        ExhibitionServiceEntity entity = new ExhibitionServiceEntity();
        entity.setId(UUID.randomUUID());
        entity.setSlug(slug);
        entity.setName("CSE 2026");
        entity.setDescription("desc");
        entity.setStartDate(LocalDate.of(2026, 2, 1));
        entity.setEndDate(LocalDate.of(2026, 3, 1));
        entity.setActive(true);
        entity.setPopupEnabled(false);
        return entity;
    }
}
