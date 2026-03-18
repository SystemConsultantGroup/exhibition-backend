package kr.ac.skku.scg.exhibition.exhibition.dto.request;

import jakarta.validation.constraints.Size;

public class ExhibitionListRequest {

    @Size(max = 128)
    private String q;

    @Size(max = 64)
    private String slug;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
