package server.sassedo.location.data.network.response;

public class CityResponse {
    private Long id;
    private String nameEn;
    private String nameBg;
    private Long countryId;
    private String countryNameEn;
    private String countryNameBg;
    private long listingCount;
    private boolean hasImage;

    public CityResponse(Long id, String nameEn, String nameBg, Long countryId,
                        String countryNameEn, String countryNameBg) {
        this(id, nameEn, nameBg, countryId, countryNameEn, countryNameBg, 0L, false);
    }

    public CityResponse(Long id, String nameEn, String nameBg, Long countryId,
                        String countryNameEn, String countryNameBg, long listingCount, boolean hasImage) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.countryId = countryId;
        this.countryNameEn = countryNameEn;
        this.countryNameBg = countryNameBg;
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

    public String getCountryNameEn() {
        return countryNameEn;
    }

    public void setCountryNameEn(String countryNameEn) {
        this.countryNameEn = countryNameEn;
    }

    public String getCountryNameBg() {
        return countryNameBg;
    }

    public void setCountryNameBg(String countryNameBg) {
        this.countryNameBg = countryNameBg;
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
