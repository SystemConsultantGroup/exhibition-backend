package kr.ac.skku.scg.exhibition.media.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionServiceEntity;
import kr.ac.skku.scg.exhibition.item.domain.ItemEntity;
import kr.ac.skku.scg.exhibition.media.domain.MediaAssetEntity;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;

@WebMvcTest(controllers = MediaController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(SecurityConfig.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void listMediaByItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        MediaAssetEntity media = media(itemId);
        given(mediaService.listByItem(itemId)).willReturn(List.of(media));

        mockMvc.perform(get("/items/{id}/media", itemId))
            .andExpect(status().isOk())
            .andDo(document("media-list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("id").description("항목 ID"))));
    }

    @Test
    void getMediaFile() throws Exception {
        UUID itemId = UUID.randomUUID();
        MediaAssetEntity media = media(itemId);
        given(mediaService.getObject(media.getId())).willReturn(new MediaService.MediaObject(media, "abc".getBytes()));

        mockMvc.perform(get("/media/{id}", media.getId()))
            .andExpect(status().isOk())
            .andExpect(content().bytes("abc".getBytes()))
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andDo(document("media-get",
                preprocessRequest(prettyPrint()),
                pathParameters(parameterWithName("id").description("미디어 ID"))));
    }

    private MediaAssetEntity media(UUID itemId) {
        ItemEntity item = new ItemEntity();
        item.setId(itemId);

        ExhibitionServiceEntity exhibition = new ExhibitionServiceEntity();
        exhibition.setId(UUID.randomUUID());

        MediaAssetEntity media = new MediaAssetEntity();
        media.setId(UUID.randomUUID());
        media.setItem(item);
        media.setExhibition(exhibition);
        media.setObjectKey("exhibitions/a/items/b/cover.jpg");
        media.setMediaType("image/jpeg");
        media.setSize(3);
        return media;
    }
}
