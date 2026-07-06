package server.sassedo.promotion.common;

public enum PromotionType {
    PROMOTED(1),
    FEATURED(2);

    private final int priority;

    PromotionType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
