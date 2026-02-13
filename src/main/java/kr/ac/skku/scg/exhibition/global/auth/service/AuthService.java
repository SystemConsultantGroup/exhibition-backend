package kr.ac.skku.scg.exhibition.global.auth.service;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.client.KakaoAuthClient;
import kr.ac.skku.scg.exhibition.global.auth.client.KakaoUserProfile;
import kr.ac.skku.scg.exhibition.global.auth.dto.request.RegisterRequest;
import kr.ac.skku.scg.exhibition.global.auth.dto.response.AuthTokenResponse;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public AuthService(KakaoAuthClient kakaoAuthClient, UserRepository userRepository, JwtTokenService jwtTokenService) {
        this.kakaoAuthClient = kakaoAuthClient;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthTokenResponse loginWithKakao(String code) {
        KakaoUserProfile profile = kakaoAuthClient.getUserProfile(code);
        String ci = "kakao:" + profile.providerId();

        UserEntity user = userRepository.findByCi(ci)
                .orElseGet(() -> createUser(ci, profile));

        return issueTokenPair(user);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(String refreshToken) {
        UUID userId = jwtTokenService.extractUserIdFromRefreshToken(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return issueTokenPair(user);
    }

    @Transactional
    public void register(UUID userId, RegisterRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.completeRegistration(
                request.name(),
                request.email(),
                request.department(),
                request.phoneNumber(),
                request.studentNumber(),
                request.userType().toUserType()
        );
    }

    private UserEntity createUser(String ci, KakaoUserProfile profile) {
        String name = (profile.name() == null || profile.name().isBlank())
                ? "kakao-user-" + profile.providerId()
                : profile.name();

        UserEntity user = new UserEntity(
                null,
                ci,
                name,
                profile.email(),
                null,
                null,
                null,
                UserType.VISITOR
        );

        return userRepository.save(user);
    }

    private AuthTokenResponse issueTokenPair(UserEntity user) {
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        String newRefreshToken = jwtTokenService.createRefreshToken(user.getId());

        return new AuthTokenResponse(
                accessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenService.getAccessTokenExpirationSeconds(),
                jwtTokenService.getRefreshTokenExpirationSeconds(),
                !user.isRegistrationCompleted()
        );
    }
}
