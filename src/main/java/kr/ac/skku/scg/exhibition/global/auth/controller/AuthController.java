package kr.ac.skku.scg.exhibition.global.auth.controller;

import jakarta.validation.Valid;
import kr.ac.skku.scg.exhibition.global.auth.dto.request.KakaoLoginRequest;
import kr.ac.skku.scg.exhibition.global.auth.dto.request.RefreshTokenRequest;
import kr.ac.skku.scg.exhibition.global.auth.dto.request.RegisterRequest;
import kr.ac.skku.scg.exhibition.global.auth.dto.response.AuthTokenResponse;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/kakao/login")
    public ResponseEntity<AuthTokenResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithKakao(request.code()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody RegisterRequest request) {
        authService.register(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
