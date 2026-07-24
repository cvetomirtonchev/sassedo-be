package server.sassedo.user.service;

import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import server.sassedo.common.service.email.BrandedEmailSender;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
public class EmailVerificationService {
    private static final String VERIFICATION_SUBJECT = "Моля, потвърдете вашия имейл адрес:";
    private static final String PASSWORD_RESET_SUBJECT = "Потвърдете вашата заявка за смяна на парола";

    private final BrandedEmailSender emailSender;

    public EmailVerificationService(BrandedEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendVerificationCode(String email, String name, String code)
            throws MessagingException, UnsupportedEncodingException {
        String plainTextContent = """
                Уважаеми %s,

                Вашият код за потвърждение е:
                %s

                Благодарим ви!

                Ако не сте заявили този имейл, моля игнорирайте го.
                """.formatted(name, code);

        emailSender.send(
                email,
                VERIFICATION_SUBJECT,
                plainTextContent,
                "email/verification-code",
                Map.of("name", name, "code", code)
        );
    }

    public void sendResetPassword(String email, String name, String token)
            throws MessagingException, UnsupportedEncodingException {
        String plainTextContent = """
                Уважаеми %s,

                Вашият код за потвърждение е:
                %s

                Благодарим ви

                Ако не сте заявили този имейл, моля игнорирайте го.
                """.formatted(name, token);

        emailSender.send(
                email,
                PASSWORD_RESET_SUBJECT,
                plainTextContent,
                "email/password-reset",
                Map.of("name", name, "code", token)
        );
    }
}
