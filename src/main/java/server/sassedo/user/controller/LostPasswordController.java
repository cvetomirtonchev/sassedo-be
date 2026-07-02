package server.sassedo.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericResponse;
import server.sassedo.user.data.network.request.ResetPasswordCodeRequest;
import server.sassedo.user.data.network.request.ResetPasswordRequest;
import server.sassedo.user.data.network.response.ResetPasswordSuccess;
import server.sassedo.user.service.user.UserService;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api/lost-password")
public class LostPasswordController {

    @Autowired
    private UserService userService;

    @PostMapping("/request")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordCodeRequest resetPasswordCodeRequest) throws MessagingException, UnsupportedEncodingException {
        try {
            userService.createPasswordResetToken(resetPasswordCodeRequest.getEmail());
            return ResponseEntity.ok()
                    .body(new GenericResponse("Mail is sent"));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> savePassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.validatePasswordResetToken(request.getOtp(), request.getEmail());
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }

        if (userService.changeUserPasswordWithResetToken(request.getEmail(),request.getPassword())) {
            return ResponseEntity.ok(new ResetPasswordSuccess(true));
        } else {
            return ResponseEntity.badRequest().body("Error while updating password");
        }
    }
}
