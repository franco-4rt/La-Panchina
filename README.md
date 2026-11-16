# La-Panchina
The problem we are looking to solve with this project is that "La Panchina" doesn't have a website. This problem persists as the owners don't invest in a website. It's an educational project aimed to give experience for every team member and make business easier and more organized, also help the restaurant reach more people. The customers and administration of the restaurant are the target audience of this project. As of today the project has 5 major features: role based authentication/authorization based on if the login os from a customer or an administrator, review system where users can directly leave a review from the website, reservation management system where customers may book a table and see the availability of the restaurant and a notification system to alert customers of they're booking status through e-mail or SMS. The goal of this project is to help the user gather information while allowing customer-restaurant interactions via online deliveries and table bookings

The systems we will use and plan on improving them (we are using: https://stockandsupplyvancouver.com as a reference because "La Panchina" doesn't have a website) are: role based-authentication and authorization, sytem-website reviews and google reviews, reservation management system, menu management system and a notification system. We will use the Google Maps Embed API.

# Iteration 1 Summary & Overview
For Iteration 1, we successfully developed and deployed the Minimum Viable Product (MVP) of the La Panchina trattoria web platform. Built using Java 21, Spring Boot 4, Thymeleaf, Spring Data JPA, and PostgreSQL hosted on Render, the system establishes a secure foundation connecting customer-facing dining features with administrative operations.

# Summary of Iteration 1 features
- Authentication & Role-Based Access Control (Epic 1):
  - Secure customer registration with phone validation and automatic CUSTOMER role assignment.
  - BCrypt password hashing (zero plain-text storage).
  - State-managed login with role-based routing (/admin for managers, / for customers).
  - Session termination (/logout) with HTTP Cache-Control headers preventing back-button exploits.
  - Administrator Dashboard displaying registered users with direct navigation cards to management page.
- Table Reservations (Epic 2):
  - Interactive reservation form with date, time, party size, and customer details.
  - Business rules validation: rejection of past dates, rejection of Mondays (restaurant closed), and operating hours enforcement (1-hour cutoff before closing).
  - Administrator Booking Management table (/admin/bookings) displaying incoming reservations.
- Dynamic Menu Management (Epic 3):
  - Public categorized menu displaying dishes, descriptions, and prices across Breakfast and Dinner menus.
  - Administrator Menu Management portal (/admin/menu) enabling full CRUD operations (adding, updating, and deleting dishes).
  - Preventing negative pricing or missing categories.
- Customer Reviews System (Epic 4):
  - Public dining reviews display with star ratings and timestamps.
  - Verified customer review submission restricted to authenticated diners.
  - Each customer can submit exactly one review, with full editing capabilities for updates.
  - Administrator review moderation portal (/admin/reviews) with confirmation for deleting inappropriate reviews.
