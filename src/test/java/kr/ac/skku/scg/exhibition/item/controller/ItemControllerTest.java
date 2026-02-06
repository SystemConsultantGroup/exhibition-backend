package kr.ac.skku.scg.exhibition.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
                Instant.now(),
                Instant.now()));

        mockMvc.perform(get("/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andDo(document("items-get"));
    }

    @Test
    void list() throws Exception {
        UUID exhibitionId = UUID.randomUUID();
        when(itemService.list(any())).thenReturn(List.of(new ItemResponse(
                UUID.randomUUID(), exhibitionId, UUID.randomUUID(), null,
                "Smart Campus", "desc", Instant.now(), Instant.now())));

        mockMvc.perform(get("/items").param("exhibitionId", exhibitionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Smart Campus"))
                .andDo(document("items-list"));
    }
}
