package server.sassedo.common.data.network.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddTestimonialRequest {
    @NotBlank
    @Size(max = 2000)
    private String quoteBg;

    @NotBlank
    @Size(max = 2000)
    private String quoteEn;

    @NotBlank
    @Size(max = 200)
    private String authorBg;

    @NotBlank
    @Size(max = 200)
    private String authorEn;

    @NotBlank
    @Size(max = 200)
    private String roleBg;

    @NotBlank
    @Size(max = 200)
    private String roleEn;

    private Boolean enabled;

    public String getQuoteBg() {
        return quoteBg;
    }

    public void setQuoteBg(String quoteBg) {
        this.quoteBg = quoteBg;
    }

    public String getQuoteEn() {
        return quoteEn;
    }

    public void setQuoteEn(String quoteEn) {
        this.quoteEn = quoteEn;
    }

    public String getAuthorBg() {
        return authorBg;
    }

    public void setAuthorBg(String authorBg) {
        this.authorBg = authorBg;
    }

    public String getAuthorEn() {
        return authorEn;
    }

    public void setAuthorEn(String authorEn) {
        this.authorEn = authorEn;
    }

    public String getRoleBg() {
        return roleBg;
    }

    public void setRoleBg(String roleBg) {
        this.roleBg = roleBg;
    }

    public String getRoleEn() {
        return roleEn;
    }

    public void setRoleEn(String roleEn) {
        this.roleEn = roleEn;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
