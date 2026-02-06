package kr.ac.skku.scg.exhibition.media.dto.response;

public record MediaFileResponse(
        String fileName,
        String contentType,
        long contentLength,
        byte[] bytes
) {
}
