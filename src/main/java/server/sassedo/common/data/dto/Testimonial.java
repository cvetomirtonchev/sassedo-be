package server.sassedo.common.data.dto;

import jakarta.persistence.*;

/**
 * Homepage testimonial quote stored bilingually (Bulgarian + English) with locale fallback
 * matching {@link Faq} and {@link HeroSlide}.
 */
@Entity
@Table(name = "testimonials")
public class Testimonial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_bg", nullable = false, length = 2000)
    private String quoteBg;

    @Column(name = "quote_en", nullable = false, length = 2000)
    private String quoteEn;

    @Column(name = "author_bg", nullable = false, length = 200)
    private String authorBg;

    @Column(name = "author_en", nullable = false, length = 200)
    private String authorEn;

    @Column(name = "role_bg", nullable = false, length = 200)
    private String roleBg;

    @Column(name = "role_en", nullable = false, length = 200)
    private String roleEn;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public Testimonial() {
    }

    public String getQuote(FaqLocale locale) {
        return resolve(quoteBg, quoteEn, locale);
    }

    public String getAuthor(FaqLocale locale) {
        return resolve(authorBg, authorEn, locale);
    }

    public String getRole(FaqLocale locale) {
        return resolve(roleBg, roleEn, locale);
    }

    private static String resolve(String bg, String en, FaqLocale locale) {
        if (locale == FaqLocale.EN) {
            return en != null && !en.isBlank() ? en : bg;
        }
        return bg != null && !bg.isBlank() ? bg : en;
    }

    public Long getId() {
        return id;
    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
