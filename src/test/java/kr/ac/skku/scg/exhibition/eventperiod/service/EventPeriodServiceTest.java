package kr.ac.skku.scg.exhibition.eventperiod.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.eventperiod.domain.EventPeriodEntity;
import kr.ac.skku.scg.exhibition.eventperiod.dto.request.AdminEventPeriodRequest;
import kr.ac.skku.scg.exhibition.eventperiod.repository.EventPeriodRepository;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventPeriodServiceTest {
    @Mock EventPeriodRepository repository;
    @Mock AdminAuthorizationService authorization;
    private EventPeriodService service;

    @BeforeEach
    void setUp() { service = new EventPeriodService(repository, authorization); }

    @Test
    void create_allowsAssignedSubAdmin() {
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        AdminEventPeriodRequest request = request("2026 발표회", "2026-01-01T00:00:00Z", "2026-12-31T00:00:00Z");
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(exhibition, request, subAdmin);

        verify(authorization).requireExhibitionAdmin(subAdmin, exhibitionId);
        verify(repository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void update_changesScopedPeriod() {
        UUID exhibitionId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        EventPeriodEntity period = new EventPeriodEntity(periodId, exhibition, "기존",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"));
        when(repository.findByIdAndExhibition_Id(periodId, exhibitionId)).thenReturn(Optional.of(period));

        service.update(periodId, exhibitionId,
                request("수정", "2026-02-01T00:00:00Z", "2026-12-01T00:00:00Z"), user(UserType.SUB_ADMIN));

        assertThat(period.getName()).isEqualTo("수정");
        verify(authorization).requireExhibitionAdmin(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(exhibitionId));
    }

    @Test
    void delete_removesScopedPeriod() {
        UUID exhibitionId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        EventPeriodEntity period = new EventPeriodEntity(periodId, exhibition, "2026",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"));
        when(repository.findByIdAndExhibition_Id(periodId, exhibitionId)).thenReturn(Optional.of(period));

        service.delete(periodId, exhibitionId, user(UserType.ADMIN));

        verify(repository).delete(period);
        verify(repository).flush();
    }

    @Test
    void update_stopsOnAuthorizationFailure() {
        UUID exhibitionId = UUID.randomUUID();
        AuthenticatedUser visitor = user(UserType.VISITOR);
        doThrow(new ForbiddenException("forbidden"))
                .when(authorization).requireExhibitionAdmin(visitor, exhibitionId);

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), exhibitionId,
                request("2026", "2026-01-01T00:00:00Z", "2026-12-01T00:00:00Z"), visitor))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void create_rejectsInvalidPeriod() {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        AdminEventPeriodRequest request = request("2026 발표회", "2026-12-31T00:00:00Z", "2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> service.create(exhibition, request, user(UserType.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime");
    }

    private AdminEventPeriodRequest request(String name, String start, String end) {
        return new AdminEventPeriodRequest(name, Instant.parse(start), Instant.parse(end));
    }

    private AuthenticatedUser user(UserType type) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", type, null, null, null, null, true);
    }
}
