package group3.la_panchina.services;

// Text message ready to be sent (to = Phone number in international format (Twilio's accepted format), e.g. +1XXXXXXXXXX)
public record SmsMessage(String to, String body) {}