package server.sassedo.common.data.network.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddFaqRequest {
    @NotBlank
    @Size(max = 1000)
    private String questionBg;

    @NotBlank
    @Size(max = 1000)
    private String questionEn;

    @NotBlank
    private String answerBg;

    @NotBlank
    private String answerEn;

    @Min(0)
    private int sortOrder;

    public String getQuestionBg() {
        return questionBg;
    }

    public void setQuestionBg(String questionBg) {
        this.questionBg = questionBg;
    }

    public String getQuestionEn() {
        return questionEn;
    }

    public void setQuestionEn(String questionEn) {
        this.questionEn = questionEn;
    }

    public String getAnswerBg() {
        return answerBg;
    }

    public void setAnswerBg(String answerBg) {
        this.answerBg = answerBg;
    }

    public String getAnswerEn() {
        return answerEn;
    }

    public void setAnswerEn(String answerEn) {
        this.answerEn = answerEn;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
