package kr.ac.skku.scg.exhibition.item.dto.response;

public record ItemBulkUploadResponse(
        int createdItems,
        int createdMediaAssets,
        int createdClassificationMappings
) {
}
