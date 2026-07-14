package server.sassedo.common.data.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "frequently_asked_questions")
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_bg", length = 1000)
    private String questionBg;

    @Column(name = "question_en", length = 1000)
    private String questionEn;

    @Lob
    @Column(name = "answer_bg", columnDefinition = "LONGTEXT")
    private String answerBg;

    @Lob
    @Column(name = "answer_en", columnDefinition = "LONGTEXT")
    private String answerEn;

    @Column(name = "sort_order")
    private int sortOrder;

    public Faq() {
    }

    public Faq(String questionBg, String questionEn, String answerBg, String answerEn, int sortOrder) {
        this.questionBg = questionBg;
        this.questionEn = questionEn;
        this.answerBg = answerBg;
        this.answerEn = answerEn;
        this.sortOrder = sortOrder;
    }

    public String getQuestion(FaqLocale locale) {
        if (locale == FaqLocale.EN) {
            return questionEn != null && !questionEn.isBlank() ? questionEn : questionBg;
        }
        return questionBg != null && !questionBg.isBlank() ? questionBg : questionEn;
    }

    public String getAnswer(FaqLocale locale) {
        if (locale == FaqLocale.EN) {
            return answerEn != null && !answerEn.isBlank() ? answerEn : answerBg;
        }
        return answerBg != null && !answerBg.isBlank() ? answerBg : answerEn;
    }

    public Long getId() {
        return id;
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
