package server.sassedo.common.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
public class BrandedEmailSender {

    private static final String SENDER_NAME = "Sassedo";
    private static final String LOGO_CONTENT_ID = "sassedoLogo";
    private static final ClassPathResource LOGO_RESOURCE =
            new ClassPathResource("email/sassedo-logo-white.png");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String currentEmail;

    public BrandedEmailSender(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${spring.mail.username}") String currentEmail
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.currentEmail = currentEmail;
    }

    public void send(
            String email,
            String subject,
            String plainTextContent,
            String templateName,
            Map<String, ?> templateVariables
    ) throws MessagingException, UnsupportedEncodingException {
        Context context = new Context(Locale.forLanguageTag("bg"));
        templateVariables.forEach(context::setVariable);
        String htmlContent = templateEngine.process(templateName, context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(currentEmail, SENDER_NAME);
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(plainTextContent, htmlContent);
        helper.addInline(LOGO_CONTENT_ID, LOGO_RESOURCE, "image/png");
        mailSender.send(message);
    }
}
