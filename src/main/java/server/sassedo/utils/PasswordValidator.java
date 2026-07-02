package server.sassedo.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator {

    /**
     * Validates if the given password is strong.
     * A strong password is defined as one that is at least 6 characters long.
     */
    public static boolean isPasswordStrong(String password) {
        String pattern = "^.{6,}$";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(password);
        return m.matches();
    }
}
