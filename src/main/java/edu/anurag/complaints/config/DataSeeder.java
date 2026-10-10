package edu.anurag.complaints.config;

import edu.anurag.complaints.model.Role;
import edu.anurag.complaints.model.User;
import edu.anurag.complaints.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account from ADMIN_EMAIL / ADMIN_PASSWORD env vars, if not already present. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.admin.email:}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set; no admin account seeded");
            return;
        }
        if (users.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPassword(encoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setDepartment("Administration");
        users.save(admin);
        log.info("Seeded admin account {}", adminEmail);
    }
}
