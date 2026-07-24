package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestimonialAdminResponse {
    private Long id;
    private String quoteBg;
    private String quoteEn;
    private String authorBg;
    private String authorEn;
    private String roleBg;
    private String roleEn;
    private boolean enabled;
}
