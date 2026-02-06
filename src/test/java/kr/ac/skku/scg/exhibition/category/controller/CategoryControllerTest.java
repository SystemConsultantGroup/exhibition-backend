package kr.ac.skku.scg.exhibition.category.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import kr.ac.skku.scg.exhibition.category.dto.CategoryDtos.CategoryTreeNode;
import kr.ac.skku.scg.exhibition.category.service.CategoryService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getTree() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        var root = new CategoryTreeNode(UUID.randomUUID(), "Software", 0, "a", 0, List.of());
        given(categoryService.buildTreeResponse(exhibitionId)).willReturn(List.of(root));

        mockMvc.perform(get("/exhibitions/{id}/categories/tree", exhibitionId))
            .andExpect(status().isOk())
            .andDo(document("categories-tree",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("전시 ID"))));
    }
}
