package group3.la_panchina.services;

// Thrown when a sms couldn't be sent (caller marks it as FAILED)
public class SmsSendException extends RuntimeException{
    public SmsSendException(String message)
    {
        super(message);
    }

    public SmsSendException(String message, Throwable cause)
    {
        super(message, cause);        
    }
}
