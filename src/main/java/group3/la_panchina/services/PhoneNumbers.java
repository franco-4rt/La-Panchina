package group3.la_panchina.services;

import java.util.Optional;

// Converts the phone numbers saved by customer(e.g (604) 321-4567) into international format Twilio needs (+16043214567).
// Only Canadian/US numbers are supported: Anything else is considered invalid and the SMS is skipped.
// It does not change what is saved in the database, the conversion is done only when an SMS is going to be sent.
public final class PhoneNumbers 
{
    private PhoneNumbers() {};
    
    // Returns the numbers as +1XXXXXXXXXX, or empty if it is a valid Canadian/US number
    public static Optional<String> toE164(String phone)
    {
        if (phone == null)
        {
            return Optional.empty();
        }

        // Keep digits only (removes spaces, dashes, dots, parenthesis, and the '+')
        String digits = phone.replaceAll("\\D", "");

        // Drop the country code if it was written (+1 ...)
        if (digits.length() == 11 && digits.startsWith("1"))
        {
            digits = digits.substring(1);
        }

        // Must be exactly 10 digits now (rejects other countries, extensions, etc.)
        if (digits.length() != 10)
        {
            return Optional.empty();
        }

        // Area code and exchange can't start with 0 or 1 (e.g. 123-456-7890 and 098-765-4321 are imposible)
        if (!digits.matches("[2-9]\\d{2}[2-9]\\d{6}"))
        {
            return Optional.empty();
        }

        // 555-0100 to 555-0199 are reserved as placeholders, do not consider
        if (digits.startsWith("555", 3) && digits.charAt(6) == '0' && digits.charAt(7) == '1')
        {
            return Optional.empty();
        }

        return Optional.of("+1" + digits);
    }
}
