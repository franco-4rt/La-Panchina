package group3.la_panchina.services;

public class PasswordValidator {
    public static final int MIN_LENGTH = 12;

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUpper && hasLower && hasDigit;
    }

    public static String getErrorMsg(){
        return "Password must be at least 12 characters long and contain at least one uppercase letter, one lower case letter and one number.";
    }
}
