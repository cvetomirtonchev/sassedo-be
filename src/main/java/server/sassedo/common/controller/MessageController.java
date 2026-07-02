package server.sassedo.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.sassedo.common.data.network.request.EmailMessageRequest;
import server.sassedo.common.service.message.MessageService;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/admin/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody EmailMessageRequest emailMessageRequest) {
        messageService.sendEmail(emailMessageRequest);
        return ResponseEntity.ok().build();
    }
}
