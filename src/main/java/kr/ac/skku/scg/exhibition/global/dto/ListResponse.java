package kr.ac.skku.scg.exhibition.global.dto;

import java.util.List;

public record ListResponse<T>(List<T> items, int page, int pageSize, long total) {

    public static <T> ListResponse<T> of(List<T> items) {
        return new ListResponse<>(items, 1, items.size(), items.size());
    }
}
