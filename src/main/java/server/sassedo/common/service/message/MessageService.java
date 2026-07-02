package server.sassedo.common.service.message;


import server.sassedo.common.data.network.request.EmailMessageRequest;

public interface MessageService {
    void sendEmail(EmailMessageRequest emailMessageRequest);
}
