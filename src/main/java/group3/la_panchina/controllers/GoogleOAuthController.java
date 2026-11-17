package group3.la_panchina.controllers;

import group3.la_panchina.models.GoogleUsersInfo;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class GoogleOAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RestTemplate restTemplate;

    @Value("${google.oauth.client-id:mock-client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret:mock-client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:8080/login/oauth2/code/google}")
    private String redirectUri;

    public GoogleOAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    // Redirect user to Google login page
    @GetMapping("/oauth2/authorization/google")
    public String redirectToGoogle() {
        String url = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("prompt", "select_account")
                .toUriString();

        return "redirect:" + url;
    }

    // Google sends the user back with an auth code
    @GetMapping("/login/oauth2/code/google")
    public String handleGoogleCallback(@RequestParam("code") String code,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Exchange auth code first for Google access token
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("code", code);
            tokenParams.add("client_id", clientId);
            tokenParams.add("client_secret", clientSecret);
            tokenParams.add("redirect_uri", redirectUri);
            tokenParams.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", tokenRequest, Map.class);

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            // Then use the token to fetch user's profile info
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<GoogleUsersInfo> userResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    userRequest,
                    GoogleUsersInfo.class);

            GoogleUsersInfo userData = userResponse.getBody();
            if (userData == null || userData.getEmail() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unable to retrieve Google account info.");
                return "redirect:/login";
            }

            // Check if user already exists in DB, if not, register
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(userData.getEmail());
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // Create new customer account
                user = new User();
                user.setName(userData.getName() != null ? userData.getName() : "Google User");
                user.setEmail(userData.getEmail());
                user.setRole("CUSTOMER");
                user.setPhone("604-555-0100");
                user.setContactPreference("Email");
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user = userRepository.save(user);
            }

            // Save to session using session_user, login the user
            session.setAttribute("session_user", user);
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, " + user.getName() + "!");

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return "redirect:/admin";
            } else {
                return "redirect:/";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Google login failed. Please try again.");
            return "redirect:/login";
        }
    }
}