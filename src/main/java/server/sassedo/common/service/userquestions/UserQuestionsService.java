package server.sassedo.common.service.userquestions;

import server.sassedo.common.data.dto.UserQuestion;
import server.sassedo.common.data.network.request.UserQuestionRequest;

import java.util.List;

public interface UserQuestionsService {
    UserQuestion addQuestion(UserQuestionRequest userQuestion);

    List<UserQuestion> updateStatus(List<Long> questions);

    List<UserQuestion> getAllQuestions();
}
