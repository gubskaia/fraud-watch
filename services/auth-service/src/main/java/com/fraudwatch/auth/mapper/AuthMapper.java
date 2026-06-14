package com.fraudwatch.auth.mapper;

import com.fraudwatch.auth.domain.Permission;
import com.fraudwatch.auth.domain.Role;
import com.fraudwatch.auth.domain.User;
import com.fraudwatch.auth.dto.UserResponse;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserResponse toUserResponse(User user) {
        Set<String> roles = user.getRoles()
            .stream()
            .map(Role::getCode)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Set<String> permissions = user.getRoles()
            .stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name(),
            roles,
            permissions
        );
    }
}

