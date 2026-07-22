package server.sassedo.promotion.data.network.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.PromotionType;

@Getter
@Setter
public class PromotionPackageRequest {

    @NotBlank
    @Size(max = 120)
    private String nameEn;

    @NotBlank
    @Size(max = 120)
    private String nameBg;

    @Size(max = 500)
    private String description;

    @NotNull
    private PromotionType type;

    @Min(1)
    private int durationDays;

    @Min(0)
    private int priceCents;

    @Size(max = 3)
    private String currency = "EUR";

    private int sortPriority = 0;

    private Boolean active;
}
