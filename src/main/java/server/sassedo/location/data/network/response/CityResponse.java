package server.sassedo.location.data.network.response;

public class CityResponse {
    private Long id;
    private String nameEn;
    private String nameBg;
    private Long countryId;
    private long listingCount;
    private boolean hasImage;

    public CityResponse(Long id, String nameEn, String nameBg, Long countryId) {
        this(id, nameEn, nameBg, countryId, 0L, false);
    }

    public CityResponse(Long id, String nameEn, String nameBg, Long countryId, long listingCount, boolean hasImage) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.countryId = countryId;
        this.listingCount = listingCount;
        this.hasImage = hasImage;
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

    public long getListingCount() {
        return listingCount;
    }

    public void setListingCount(long listingCount) {
        this.listingCount = listingCount;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }
}
