package kr.ac.skku.scg.exhibition.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void me() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(
                userId,
                "kakao:12345",
                "홍길동",
                "hong@example.com",
                "소프트웨어학과",
                "010-1234-5678",
                "2020123456",
                UserType.STUDENT
        );
        user.completeRegistration(
                "홍길동",
                "hong@example.com",
                "소프트웨어학과",
                "010-1234-5678",
                "2020123456"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/me").requestAttr("auth.userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("hong@example.com"))
                .andExpect(jsonPath("$.department").value("소프트웨어학과"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.studentNumber").value("2020123456"))
                .andExpect(jsonPath("$.registrationCompleted").value(true))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.ci").doesNotExist());
    }
}
