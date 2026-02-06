package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemVisibility;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;

@WebMvcTest(controllers = ItemController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(SecurityConfig.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    void listItems() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        ItemEntity item = item(exhibitionId);
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        given(itemService.list(exhibitionId, null, null, null, null, null, pageable))
            .willReturn(new PageImpl<>(List.of(item), pageable, 1));

        mockMvc.perform(get("/exhibitions/{id}/items", exhibitionId))
            .andExpect(status().isOk())
            .andDo(document("items-list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("전시 ID")),
                queryParameters(
                    parameterWithName("category_id").optional().description("카테고리 ID"),
                    parameterWithName("q").optional().description("검색어"),
                    parameterWithName("visibility").optional().description("노출 타입"),
                    parameterWithName("published").optional().description("게시 여부"),
                    parameterWithName("classification").optional().description("분류명"),
                    parameterWithName("page").optional().description("페이지"),
                    parameterWithName("page_size").optional().description("페이지 크기")
                )));
    }

    @Test
    void getItem() throws Exception {
        ItemEntity item = item(UUID.randomUUID());
        given(itemService.get(item.getId())).willReturn(item);

        mockMvc.perform(get("/items/{id}", item.getId()))
            .andExpect(status().isOk())
            .andDo(document("items-get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("항목 ID"))));
    }

    private ItemEntity item(UUID exhibitionId) {
        ExhibitionServiceEntity exhibition = new ExhibitionServiceEntity();
        exhibition.setId(exhibitionId);
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());

        ItemEntity item = new ItemEntity();
        item.setId(UUID.randomUUID());
        item.setExhibition(exhibition);
        item.setCategory(category);
        item.setTitle("Smart Campus");
        item.setVisibility(ItemVisibility.PUBLIC);
        return item;
    }
}
