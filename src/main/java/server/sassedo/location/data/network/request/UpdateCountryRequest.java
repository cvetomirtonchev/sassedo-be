package server.sassedo.location.data.network.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateCountryRequest {
    @NotNull
    private Long id;

    @Size(max = 100)
    private String nameEn;

    @Size(max = 100)
    private String nameBg;

    @Size(max = 10)
    private String code;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameBg() {
        return nameBg;
    }

    public void setNameBg(String nameBg) {
        this.nameBg = nameBg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
