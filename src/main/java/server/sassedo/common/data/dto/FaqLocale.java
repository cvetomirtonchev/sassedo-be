package server.sassedo.common.data.dto;

import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

public enum FaqLocale {
    BG,
    EN;

    public static FaqLocale fromCode(String code) throws GenericException {
        if (code == null) {
            throw new GenericException(GenericExceptionCode.FAQ_INVALID_LOCALE, "Locale is required");
        }
        switch (code.trim().toLowerCase()) {
            case "bg":
                return BG;
            case "en":
                return EN;
            default:
                throw new GenericException(GenericExceptionCode.FAQ_INVALID_LOCALE,
                        "Unsupported locale: " + code);
        }
    }
}
