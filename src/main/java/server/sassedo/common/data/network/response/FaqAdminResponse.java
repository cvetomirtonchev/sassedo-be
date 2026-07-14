package server.sassedo.common.data.network.response;

public class FaqAdminResponse {
    private Long id;
    private String questionBg;
    private String questionEn;
    private String answerBg;
    private String answerEn;
    private int sortOrder;

    public FaqAdminResponse(Long id, String questionBg, String questionEn,
                            String answerBg, String answerEn, int sortOrder) {
        this.id = id;
        this.questionBg = questionBg;
        this.questionEn = questionEn;
        this.answerBg = answerBg;
        this.answerEn = answerEn;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
