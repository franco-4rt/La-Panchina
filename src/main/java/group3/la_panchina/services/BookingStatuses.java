package group3.la_panchina.services;

// Reads the booking status (a free-text field) without depending on upper/lower case.
public final class BookingStatuses {
    
    private BookingStatuses() 
    {
        // Private constructor to prevent instantiation
    }

    // True for "Confirmed" (case-insensitive)
    public static boolean isConfirmed(String status) 
    {
        return "confirmed".equalsIgnoreCase(status);
    }

    // True for "Cancelled" or "Canceled" (case-insensitive)
    public static boolean isCancelled(String status) 
    {
        return "cancelled".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status);
    }
}
