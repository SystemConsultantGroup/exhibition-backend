package kr.ac.skku.scg.exhibition.exhibition.dto.request;

import jakarta.validation.constraints.Size;

public class ExhibitionListRequest {

    @Size(max = 128)
    private String q;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }
}
