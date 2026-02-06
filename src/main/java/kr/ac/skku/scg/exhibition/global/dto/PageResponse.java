package kr.ac.skku.scg.exhibition.global.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total
) {
}
