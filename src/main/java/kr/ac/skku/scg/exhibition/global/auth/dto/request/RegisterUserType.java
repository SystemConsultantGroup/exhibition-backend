package kr.ac.skku.scg.exhibition.global.auth.dto.request;

import kr.ac.skku.scg.exhibition.user.domain.UserType;

public enum RegisterUserType {
    VISITOR,
    STUDENT,
    STAFF,
    PROFESSOR;

    public UserType toUserType() {
        return UserType.valueOf(name());
    }
}
