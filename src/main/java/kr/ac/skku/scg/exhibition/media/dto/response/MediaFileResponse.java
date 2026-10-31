package kr.ac.skku.scg.exhibition.media.dto.response;

import org.springframework.core.io.Resource;

public record MediaFileResponse(
        String fileName,
        String contentType,
        long contentLength,
        Resource resource
) {
}
