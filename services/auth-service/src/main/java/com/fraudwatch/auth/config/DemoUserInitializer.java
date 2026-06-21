package com.fraudwatch.auth.config;

import com.fraudwatch.auth.domain.Role;
import com.fraudwatch.auth.domain.User;
import com.fraudwatch.auth.domain.UserStatus;
import com.fraudwatch.auth.repository.RoleRepository;
import com.fraudwatch.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(
            "admin.demo",
            "admin@fraudwatch.local",
            "AdminPass123!",
            "Platform",
            "Admin",
            "ROLE_ADMIN"
        );
        seedUser(
            "analyst.demo",
            "analyst@fraudwatch.local",
            "AnalystPass123!",
            "Fraud",
            "Analyst",
            "ROLE_ANALYST"
        );
    }

    private void seedUser(
        String username,
        String email,
        String rawPassword,
        String firstName,
        String lastName,
        String roleCode
    ) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }

        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new DemoUserInitializationException("Required role is missing: " + roleCode));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.getRoles().add(role);

        userRepository.save(user);
    }

    private static final class DemoUserInitializationException extends RuntimeException {
        private DemoUserInitializationException(String message) {
            super(message);
        }
    }
}
