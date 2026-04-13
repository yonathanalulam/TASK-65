package com.culinarycoach.config;

import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.auth.PasswordHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;

    @Value("${app.bootstrap.admin-password:#{null}}")
    private String bootstrapAdminPassword;

    public DataInitializer(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            PasswordHistoryService passwordHistoryService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryService = passwordHistoryService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
            if (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
                log.error("No admin user exists and no bootstrap password configured. "
                    + "Set APP_BOOTSTRAP_ADMIN_PASSWORD environment variable to create the initial admin.");
                return;
            }

            log.info("Creating default admin user...");

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found in database"));

            String encoded = passwordEncoder.encode(bootstrapAdminPassword);

            User admin = new User();
            admin.setUsername("admin");
            admin.setDisplayName("System Admin");
            admin.setEmail("admin@culinarycoach.local");
            admin.setPasswordHash(encoded);
            admin.setStatus(AccountStatus.ACTIVE);
            admin.setForcePasswordChange(true);
            admin.setRoles(Set.of(adminRole));

            User saved = userRepository.save(admin);
            passwordHistoryService.recordPassword(saved.getId(), encoded);

            log.info("Default admin user created. Password change required on first login.");
        }
    }
}
