package server.sassedo.location.data.network.response;

public class CityResponse {
    private Long id;
    private String nameEn;
    private String nameBg;
    private Long countryId;

    public CityResponse(Long id, String nameEn, String nameBg, Long countryId) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.countryId = countryId;
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

    public Long getCountryId() {
        return countryId;
    }

    public void setCountryId(Long countryId) {
        this.countryId = countryId;
    }
}
