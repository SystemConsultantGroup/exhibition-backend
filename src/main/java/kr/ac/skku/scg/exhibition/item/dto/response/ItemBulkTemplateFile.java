package kr.ac.skku.scg.exhibition.item.dto.response;

public record ItemBulkTemplateFile(
        String fileName,
        byte[] bytes
) {
}
