package kr.ac.skku.scg.exhibition.eventperiod.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
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
                .andDo(document("event-periods-get"));
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
                .andExpect(jsonPath("$[0].name").value("2025-1학기"))
                .andDo(document("event-periods-list"));
    }
}
