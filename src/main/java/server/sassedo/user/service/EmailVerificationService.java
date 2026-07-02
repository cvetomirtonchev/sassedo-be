package server.sassedo.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
public class EmailVerificationService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String currentEmail;

    public void sendVerificationCode(String email, String name, String code) throws MessagingException, UnsupportedEncodingException {
        String senderName = "Sassedo";
        String subject = "Моля, потвърдете вашия имейл адрес:";
        String content = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<style>"
                + "body {font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f7f7f7;}"
                + ".container {padding: 20px; background-color: #ffffff; max-width: 600px; margin: auto; border-radius: 10px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);}"
                + ".header {background-color: #D9B451FF; padding: 10px 20px; border-top-left-radius: 10px; border-top-right-radius: 10px; text-align: center; color: #ffffff;}"
                + ".content {padding: 20px;}"
                + ".button {display: inline-block; padding: 10px 20px; font-size: 16px; color: #ffffff; background-color: #4CAF50; text-decoration: none; border-radius: 5px;}"
                + ".footer {text-align: center; padding: 10px 0; font-size: 12px; color: #888888;}"
                + ".token-box {display: inline-block; padding: 10px 20px; font-size: 18px; color: #333333; background-color: #f1f1f1; border: 1px dashed #494848FF; border-radius: 5px; margin: 10px 0; user-select: all;}"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'><h1>Потвърдете вашия имейл</h1></div>"
                + "<div class='content'>"
                + "<p>Уважаеми [[name]],</p>"
                + "<p>Вашият код за потвърждение е:</p>"
                + "<div class='token-box'>" + code + "</div>"
                + "<p>Благодарим ви!"
                + "</div>"
                + "<div class='footer'>Ако не сте заявили този имейл, моля игнорирайте го.</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(currentEmail, senderName);
        helper.setTo(email);
        helper.setSubject(subject);
        content = content.replace("[[name]]", name);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendResetPassword(String email, String name, String token) throws MessagingException, UnsupportedEncodingException {
        String senderName = "Sassedo";
        String subject = "Потвърдете вашата заявка за смяна на парола";
        String content = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<style>"
                + "body {font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f7f7f7;}"
                + ".container {padding: 20px; background-color: #ffffff; max-width: 600px; margin: auto; border-radius: 10px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);}"
                + ".header {background-color: #D9B451FF; padding: 10px 20px; border-top-left-radius: 10px; border-top-right-radius: 10px; text-align: center; color: #ffffff;}"
                + ".content {padding: 20px;}"
                + ".token-box {display: inline-block; padding: 10px 20px; font-size: 18px; color: #333333; background-color: #f1f1f1; border: 1px dashed #494848FF; border-radius: 5px; margin: 10px 0; user-select: all;}"
                + ".footer {text-align: center; padding: 10px 0; font-size: 12px; color: #888888;}"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'><h1>Потвърдете вашата заявка за смяна на парола</h1></div>"
                + "<div class='content'>"
                + "<p>Уважаеми [[name]],</p>"
                + "<p>Вашият код за потвърждение е:</p>"
                + "<div class='token-box'>" + token + "</div>"
                + "<p>Благодарим ви</p>"
                + "</div>"
                + "<div class='footer'>Ако не сте заявили този имейл, моля игнорирайте го.</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(currentEmail, senderName);
        helper.setTo(email);
        helper.setSubject(subject);
        content = content.replace("[[name]]", name);
        helper.setText(content, true);
        mailSender.send(message);
    }
}
