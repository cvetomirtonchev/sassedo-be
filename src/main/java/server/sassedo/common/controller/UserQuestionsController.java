package server.sassedo.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.UserQuestion;
import server.sassedo.common.data.network.request.UpdateUserQuestionStatusRequest;
import server.sassedo.common.data.network.request.UserQuestionRequest;
import server.sassedo.common.data.network.response.UserQuestionResponse;
import server.sassedo.common.service.userquestions.UserQuestionsService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
public class UserQuestionsController {

    @Autowired
    private UserQuestionsService userQuestionService;

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody UserQuestionRequest request) {
        return ResponseEntity.ok(convertToRequest(userQuestionService.addQuestion(request)));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllQuestions() {
        List<UserQuestionResponse> userQuestions = userQuestionService.getAllQuestions()
                .stream()
                .map(this::convertToRequest)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userQuestions);
    }

    @PutMapping("/admin/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody UpdateUserQuestionStatusRequest request) {
        List<UserQuestionResponse> userQuestions = userQuestionService.updateStatus(request.getQuestions())
                .stream()
                .map(this::convertToRequest)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userQuestions);
    }

    private UserQuestionResponse convertToRequest(UserQuestion userQuestion) {
        return new UserQuestionResponse(
                userQuestion.getId(),
                userQuestion.getName(),
                userQuestion.getEmail(),
                userQuestion.getChildrenName(),
                userQuestion.getQuestion(),
                userQuestion.getStatus()
        );
    }
}
