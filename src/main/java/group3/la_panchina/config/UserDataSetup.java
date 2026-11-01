package group3.la_panchina.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;

@Component 
public class UserDataSetup implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value ("${ADMIN_EMAIL:admin@lapanchina.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    public UserDataSetup(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Automatically create admin using the configured credentials
        if(!userRepository.existsByEmailIgnoreCase(adminEmail)){
            User admin = new User(
                "La Panchina Admin",
                adminEmail,
                passwordEncoder.encode(adminPassword),
                "ADMIN"
            );
            userRepository.save(admin);
            System.out.println("Admin account created: " + adminEmail);
        }

        // Sameple customer for local testing
        if(!userRepository.existsByEmailIgnoreCase("customer@test.com")){
            User customer = new User(
                "Josiah Leung",
                "customer@test.com",
                passwordEncoder.encode("password123"),
                "CUSTOMER"
            );
            userRepository.save(customer);
            System.out.println("Tetsing customer created: customer@test.com");
        }
    }
}
