package server.sassedo.common.data.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "helper_texts")
public class HelperText {
    @Id
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String value;

    public HelperText() {
    }

    public HelperText(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
