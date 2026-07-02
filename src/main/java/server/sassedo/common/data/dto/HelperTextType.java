package server.sassedo.common.data.dto;

public enum HelperTextType {
    TERMS_AND_CONDITIONS(1L),
    PRIVACY_POLICY(2L),
    GDPR(3L),
    WHO_WE_ARE(4L),
    OUR_TEAM(5L),
    MAIN_TITLE(6L),
    MAIN_TEXT(7L);

    public final long id;

    HelperTextType(long id) {
        this.id = id;
    }

    public static HelperTextType fromId(long id) {
        for (HelperTextType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("No HelperTextType with ID " + id);
    }
}
