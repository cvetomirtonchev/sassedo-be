package server.sassedo.user.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import server.sassedo.common.service.email.BrandedEmailSender;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    private static final String SENDER_EMAIL = "no-reply@sassedo.test";

    @Mock
    private JavaMailSender mailSender;

    private MimeMessage mimeMessage;
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailVerificationService = new EmailVerificationService(
                new BrandedEmailSender(mailSender, createTemplateEngine(), SENDER_EMAIL)
        );
    }

    @Test
    void sendRegistrationSuccess_rendersBrandedEmailWithoutAVerificationCode() throws Exception {
        emailVerificationService.sendRegistrationSuccess(
                "new-user@example.com",
                "Мария"
        );

        mimeMessage.saveChanges();
        verify(mailSender).send(mimeMessage);
        assertEnvelope(
                "new-user@example.com",
                "Успешна регистрация в Sassedo"
        );

        String plainText = findTextPart(mimeMessage, "text/plain");
        String html = findTextPart(mimeMessage, "text/html");

        assertThat(plainText)
                .contains("Регистрацията ви в Sassedo беше успешна.")
                .contains("поискайте код за потвърждение")
                .doesNotContain("000000");
        assertThat(html)
                .contains("Успешна регистрация")
                .contains("Мария")
                .contains("Профилът ви в Sassedo беше създаден успешно.")
                .contains("cid:sassedoLogo")
                .doesNotContain("Вашият код за потвърждение е");
        assertLogoPart();
    }

    @Test
    void sendVerificationCode_rendersBrandedMultipartEmailAndEscapesName() throws Exception {
        String unsafeName = "Иван <script>alert('x')</script>";

        emailVerificationService.sendVerificationCode(
                "user@example.com",
                unsafeName,
                "123456"
        );

        mimeMessage.saveChanges();
        verify(mailSender).send(mimeMessage);
        assertEnvelope(
                "user@example.com",
                "Моля, потвърдете вашия имейл адрес:"
        );

        String plainText = findTextPart(mimeMessage, "text/plain");
        String html = findTextPart(mimeMessage, "text/html");

        assertThat(plainText)
                .contains(unsafeName)
                .contains("123456")
                .contains("Ако не сте заявили този имейл, моля игнорирайте го.");
        assertThat(html)
                .contains("Потвърдете вашия имейл")
                .contains("Иван &lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
                .doesNotContain("<script>")
                .contains("123456")
                .contains("#033c24")
                .contains("#eaf6e9")
                .contains("border-radius: 14px")
                .contains("cid:sassedoLogo")
                .contains("alt=\"Sassedo\"")
                .contains("bgcolor=\"#033c24\"")
                .contains("width: 600px; max-width: 600px")
                .contains("@media screen and (max-width: 620px)")
                .contains("padding: 32px 20px !important");
        assertLogoPart();
    }

    @Test
    void sendResetPassword_rendersPasswordResetTemplateAndFallbackText() throws Exception {
        emailVerificationService.sendResetPassword(
                "reset@example.com",
                "Мария",
                "654321"
        );

        mimeMessage.saveChanges();
        verify(mailSender).send(mimeMessage);
        assertEnvelope(
                "reset@example.com",
                "Потвърдете вашата заявка за смяна на парола"
        );

        String plainText = findTextPart(mimeMessage, "text/plain");
        String html = findTextPart(mimeMessage, "text/html");

        assertThat(plainText)
                .contains("Уважаеми Мария,")
                .contains("654321")
                .contains("Благодарим ви");
        assertThat(html)
                .contains("Потвърдете вашата заявка за смяна на парола")
                .contains("Мария")
                .contains("654321")
                .contains("lang=\"bg\"")
                .contains("role=\"presentation\"")
                .contains("cid:sassedoLogo");
        assertLogoPart();
    }

    private void assertEnvelope(String recipient, String subject) throws Exception {
        InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        InternetAddress to = (InternetAddress) mimeMessage
                .getRecipients(Message.RecipientType.TO)[0];

        assertThat(from.getAddress()).isEqualTo(SENDER_EMAIL);
        assertThat(from.getPersonal()).isEqualTo("Sassedo");
        assertThat(to.getAddress()).isEqualTo(recipient);
        assertThat(mimeMessage.getSubject()).isEqualTo(subject);
        assertThat(mimeMessage.getContentType()).startsWith("multipart/mixed");
    }

    private void assertLogoPart() throws Exception {
        Part logoPart = findPartByContentId(mimeMessage, "<sassedoLogo>");

        assertThat(logoPart).isNotNull();
        assertThat(logoPart.getContentType()).startsWith("image/png");
        assertThat(logoPart.getDisposition()).isEqualTo(Part.INLINE);
    }

    private static String findTextPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                String content = findTextPart(multipart.getBodyPart(index), mimeType);
                if (content != null) {
                    return content;
                }
            }
        }

        return null;
    }

    private static Part findPartByContentId(Part part, String contentId) throws Exception {
        String[] contentIds = part.getHeader("Content-ID");
        if (contentIds != null && contentIds.length > 0 && contentId.equals(contentIds[0])) {
            return part;
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                Part match = findPartByContentId(multipart.getBodyPart(index), contentId);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private static SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
