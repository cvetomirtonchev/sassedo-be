package server.sassedo.common.service.userquestions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.UserQuestion;
import server.sassedo.common.data.dto.UserQuestionStatus;
import server.sassedo.common.data.network.request.UserQuestionRequest;
import server.sassedo.common.repository.UserQuestionsRepository;

import java.util.List;

@Service
public class UserQuestionsServiceImpl implements UserQuestionsService {

    @Autowired
    private UserQuestionsRepository userQuestionsRepository;

    @Override
    public UserQuestion addQuestion(UserQuestionRequest userQuestion) {
        UserQuestion question = new UserQuestion(userQuestion.getName(),
                userQuestion.getEmail(),
                userQuestion.getChildrenName(),
                userQuestion.getQuestion());
        return userQuestionsRepository.save(question);
    }

    @Override
    public List<UserQuestion> updateStatus(List<Long> questions) {
        List<UserQuestion> userQuestions = userQuestionsRepository.findAllById(questions);
        for (UserQuestion userQuestion : userQuestions) {
            userQuestion.setStatus(UserQuestionStatus.SEEN);
        }
        return userQuestionsRepository.saveAll(userQuestions);
    }

    @Override
    public List<UserQuestion> getAllQuestions() {
        return userQuestionsRepository.findAll();
    }
}
