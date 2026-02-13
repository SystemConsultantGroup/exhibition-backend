package kr.ac.skku.scg.exhibition.global.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.dto.response.AuthTokenResponse;
import kr.ac.skku.scg.exhibition.global.auth.service.AuthService;
import kr.ac.skku.scg.exhibition.global.config.SecurityConfig;
import kr.ac.skku.scg.exhibition.global.error.ApiExceptionHandler;
import kr.ac.skku.scg.exhibition.global.config.WebConfig;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, WebConfig.class})
@AutoConfigureRestDocs
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void kakaoLogin() throws Exception {
        when(authService.loginWithKakao(anyString()))
                .thenReturn(new AuthTokenResponse("access-token", "refresh-token", "Bearer", 3600, 1209600, true));

        mockMvc.perform(post("/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "kakao-auth-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiresIn").value(3600))
                .andExpect(jsonPath("$.refreshTokenExpiresIn").value(1209600))
                .andExpect(jsonPath("$.registrationRequired").value(true))
                .andDo(document("auth-kakao-login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("code").description("카카오 인가 코드")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("서비스 Access Token"),
                                fieldWithPath("refreshToken").description("서비스 Refresh Token"),
                                fieldWithPath("tokenType").description("토큰 타입(Bearer)"),
                                fieldWithPath("accessTokenExpiresIn").description("Access Token 만료(초)"),
                                fieldWithPath("refreshTokenExpiresIn").description("Refresh Token 만료(초)"),
                                fieldWithPath("registrationRequired").description("회원가입 정보 입력 필요 여부")
                        )));
    }

    @Test
    void refresh() throws Exception {
        when(authService.refresh(anyString()))
                .thenReturn(new AuthTokenResponse("new-access-token", "new-refresh-token", "Bearer", 3600, 1209600, false));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.registrationRequired").value(false))
                .andDo(document("auth-refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("refreshToken").description("기존 Refresh Token")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("재발급된 Access Token"),
                                fieldWithPath("refreshToken").description("재발급된 Refresh Token"),
                                fieldWithPath("tokenType").description("토큰 타입(Bearer)"),
                                fieldWithPath("accessTokenExpiresIn").description("Access Token 만료(초)"),
                                fieldWithPath("refreshTokenExpiresIn").description("Refresh Token 만료(초)"),
                                fieldWithPath("registrationRequired").description("회원가입 정보 입력 필요 여부")
                        )));
    }

    @Test
    void register() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(new UserEntity(
                        userId,
                        "kakao:123",
                        "홍길동",
                        null,
                        null,
                        null,
                        null,
                        UserType.VISITOR
                )));

        mockMvc.perform(post("/auth/register")
                        .requestAttr("auth.userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userType": "STUDENT",
                                  "name": "홍길동",
                                  "email": "hong@example.com",
                                  "department": "소프트웨어학과",
                                  "phoneNumber": "010-1234-5678",
                                  "studentNumber": "2020123456"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andDo(document("auth-register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("userType").description("회원 유형(VISITOR, STUDENT, STAFF, PROFESSOR)"),
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("email").description("이메일").optional(),
                                fieldWithPath("department").description("소속").optional(),
                                fieldWithPath("phoneNumber").description("연락처").optional(),
                                fieldWithPath("studentNumber").description("학번").optional()
                        )));
    }
}
