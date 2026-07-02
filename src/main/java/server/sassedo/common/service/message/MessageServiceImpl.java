package server.sassedo.common.service.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.network.request.EmailMessageRequest;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.repository.UserRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Async
    public void sendEmail(EmailMessageRequest emailMessageRequest) {
        String[] recipients;
        if (emailMessageRequest.getRecipient() != null && !emailMessageRequest.getRecipient().isEmpty()) {
            recipients = new String[]{emailMessageRequest.getRecipient()};

        } else {
            List<User> users = userRepository.findAll();
            recipients = users.stream()
                    .map(User::getEmail)
                    .toArray(String[]::new);
        }

        for (String recipient : recipients) {
            sendIndividualEmail(recipient, emailMessageRequest.getSubject(), emailMessageRequest.getContent());
            // TODO remove it after setting new email client
            try {
                TimeUnit.SECONDS.sleep(2); // 2 seconds delay between each email
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendIndividualEmail(String recipient, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }
}
