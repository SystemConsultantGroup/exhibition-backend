package kr.ac.skku.scg.exhibition.classification.controller;

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

import kr.ac.skku.scg.exhibition.classification.domain.ItemClassificationEntity;
import kr.ac.skku.scg.exhibition.classification.service.ClassificationService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;

@WebMvcTest(controllers = ClassificationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(SecurityConfig.class)
class ClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassificationService classificationService;

    @Test
    void listClassifications() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        ItemClassificationEntity entity = new ItemClassificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Paper");
        ExhibitionServiceEntity exhibition = new ExhibitionServiceEntity();
        exhibition.setId(exhibitionId);
        entity.setExhibition(exhibition);

        given(classificationService.list(exhibitionId)).willReturn(List.of(entity));

        mockMvc.perform(get("/exhibitions/{id}/classifications", exhibitionId))
            .andExpect(status().isOk())
            .andDo(document("classifications-list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("전시 ID"))));
    }
}
