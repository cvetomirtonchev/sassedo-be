package server.sassedo.location.data.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "cities",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name_en", "country_id"})
        })
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_bg", nullable = false)
    private String nameBg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Lob
    @Column(name = "image", columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    public City() {
    }

    public City(String nameEn, String nameBg, Country country) {
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.country = country;
    }

    public Long getId() {
        return id;
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

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }
}
