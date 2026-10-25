-- 기존 전시와 신규 전시의 기본 표시 설정을 유지한다.
-- 프론트엔드 전용 설정으로, 작품 데이터 및 조회 조건에는 영향을 주지 않는다.
ALTER TABLE exhibition_services
    ADD COLUMN show_awards BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE exhibition_services
    ADD COLUMN default_to_latest_event_period BOOLEAN NOT NULL DEFAULT FALSE;
