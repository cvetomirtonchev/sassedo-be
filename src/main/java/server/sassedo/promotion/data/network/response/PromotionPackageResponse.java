package server.sassedo.promotion.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.PromotionType;

@Getter
@Setter
public class PromotionPackageResponse {

    private Long id;
    private String name;
    private String description;
    private PromotionType type;
    private int durationDays;
    private int priceCents;
    private String currency;
    private boolean active;
    private int sortPriority;
    private boolean pinnable;
}
