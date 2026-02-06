package kr.ac.skku.scg.exhibition.item.domain;

public enum ItemVisibility {
    PUBLIC,
    PRIVATE,
    UNLISTED;

    public static ItemVisibility from(String value) {
        if (value == null || value.isBlank()) {
            return PUBLIC;
        }
        return ItemVisibility.valueOf(value.trim().toUpperCase());
    }
}
