package server.sassedo.location.data.dto;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "countries",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name_en")
        })
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_bg", nullable = false)
    private String nameBg;

    @Column(length = 10)
    private String code;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<City> cities = new ArrayList<>();

    public Country() {
    }

    public Country(String nameEn, String nameBg, String code) {
        this.nameEn = nameEn;
        this.nameBg = nameBg;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<City> getCities() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }
}
