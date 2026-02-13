package kr.ac.skku.scg.exhibition.user.controller;

import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.user.dto.response.MyProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> me(@CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(MyProfileResponse.from(currentUser));
    }
}
