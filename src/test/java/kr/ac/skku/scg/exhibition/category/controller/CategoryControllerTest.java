package kr.ac.skku.scg.exhibition.category.controller;

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
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibitionArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CategoryController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        when(categoryService.get(id, exhibitionId)).thenReturn(new CategoryResponse(id, exhibitionId, "web", "Web"));

        mockMvc.perform(get("/categories/{id}", id)
                        .requestAttr(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION, currentExhibition(exhibitionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("categories-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("카테고리 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("카테고리 ID"),
                                fieldWithPath("exhibitionId").description("전시 ID"),
                                fieldWithPath("slug").description("카테고리 슬러그"),
                                fieldWithPath("name").description("카테고리명")
                        )));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(categoryService.list(any())).thenReturn(List.of(
                new CategoryResponse(UUID.randomUUID(), exhibitionId, "ai", "AI")
        ));

        mockMvc.perform(get("/categories")
                        .requestAttr(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION, currentExhibition(exhibitionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("AI"))
                .andDo(document("categories-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("items").description("카테고리 목록"),
                                fieldWithPath("items[].id").description("카테고리 ID"),
                                fieldWithPath("items[].exhibitionId").description("전시 ID"),
                                fieldWithPath("items[].slug").description("카테고리 슬러그"),
                                fieldWithPath("items[].name").description("카테고리명"),
                                fieldWithPath("page").description("페이지 번호"),
                                fieldWithPath("pageSize").description("페이지 크기"),
                                fieldWithPath("total").description("전체 건수")
                        )));
    }

    private ExhibitionEntity currentExhibition(UUID exhibitionId) {
        return new ExhibitionEntity(exhibitionId, "sw-gp", "전시");
    }
}
