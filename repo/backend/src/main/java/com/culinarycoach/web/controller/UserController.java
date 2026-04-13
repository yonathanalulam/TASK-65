package com.culinarycoach.web.controller;

import com.culinarycoach.security.auth.AuthenticatedUserResolver;
import com.culinarycoach.service.UserService;
import com.culinarycoach.web.dto.request.CreateUserRequest;
import com.culinarycoach.web.dto.response.ApiResponse;
import com.culinarycoach.web.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserResolver userResolver;

    public UserController(UserService userService, AuthenticatedUserResolver userResolver) {
        this.userService = userService;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        UserResponse user = userService.createUser(request, adminUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<UserResponse> users = userService.listUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<UserResponse>> disableUser(
            @PathVariable Long id, Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        UserResponse user = userService.disableUser(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<UserResponse>> enableUser(
            @PathVariable Long id, Authentication authentication) {
        String adminUsername = userResolver.require(authentication).getUsername();
        UserResponse user = userService.enableUser(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
