package com.dotani.shared.exception;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Vietnamese, user-facing text for error codes (messages.properties, key "error.<CODE>"). */
@Component
public class ErrorMessages {

    public static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final MessageSource messageSource;

    public ErrorMessages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** Text for the code, or the given fallback when the code has no translation yet. */
    public String forCode(String code, Object[] args, String fallback) {
        return messageSource.getMessage("error." + code, args, fallback, VIETNAMESE);
    }

    public String forCode(String code, String fallback) {
        return forCode(code, new Object[0], fallback);
    }
}
