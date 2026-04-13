package com.culinarycoach.service;

import com.culinarycoach.audit.AuditEventType;
import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.Role;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.repository.RoleRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.auth.PasswordHistoryService;
import com.culinarycoach.security.auth.PasswordPolicyValidator;
import com.culinarycoach.web.dto.request.CreateUserRequest;
import com.culinarycoach.web.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordHistoryService passwordHistoryService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        PasswordPolicyValidator passwordPolicyValidator,
                        PasswordHistoryService passwordHistoryService,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.passwordHistoryService = passwordHistoryService;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, String adminUsername) {
        if (userRepository.existsByUsernameLower(request.username().toLowerCase())) {
            throw new IllegalArgumentException("Username already exists");
        }

        List<String> policyErrors = passwordPolicyValidator.validate(request.password(), request.username());
        if (!policyErrors.isEmpty()) {
            throw new IllegalArgumentException("Password policy: " + String.join("; ", policyErrors));
        }

        String encoded = passwordEncoder.encode(request.password());

        User user = new User();
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setPasswordHash(encoded);
        user.setStatus(AccountStatus.ACTIVE);
        user.setForcePasswordChange(true);

        Set<Role> roles = new HashSet<>();
        Set<String> requestedRoles = request.roles() != null ? request.roles() : Set.of("ROLE_USER");
        for (String roleName : requestedRoles) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        passwordHistoryService.recordPassword(saved.getId(), encoded);

        auditService.log(AuditEventType.USER_CREATED, null, adminUsername,
            null, null, "USER", saved.getId().toString(),
            "Created user: " + saved.getUsername());

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse disableUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new IllegalStateException("User is already disabled");
        }

        user.setStatus(AccountStatus.DISABLED);
        userRepository.save(user);

        auditService.log(AuditEventType.USER_DISABLED, user.getId(), adminUsername,
            null, null, "USER", userId.toString(),
            "Disabled user: " + user.getUsername());

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse enableUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() != AccountStatus.DISABLED) {
            throw new IllegalStateException("User is not disabled");
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        auditService.log(AuditEventType.USER_ENABLED, user.getId(), adminUsername,
            null, null, "USER", userId.toString(),
            "Enabled user: " + user.getUsername());

        return UserResponse.from(user);
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }
}
