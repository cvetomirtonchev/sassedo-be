package server.sassedo.location.data.network.response;

import java.util.List;

public class CountryResponse {
    private Long id;
    private String nameEn;
    private String nameBg;
    private String code;
    private List<CityResponse> cities;

    public CountryResponse(Long id, String nameEn, String nameBg, String code, List<CityResponse> cities) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.code = code;
        this.cities = cities;
    }

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

    public List<CityResponse> getCities() {
        return cities;
    }

    public void setCities(List<CityResponse> cities) {
        this.cities = cities;
    }
}
