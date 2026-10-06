package com.example.JustBuyIt.DTOs;

import com.example.JustBuyIt.Models.Role;
import com.example.JustBuyIt.Models.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class UserPrincipalDto implements UserDetails, Serializable {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean emailVerified;
    private boolean banned;
    private Instant passwordChangedAt;

    public UserPrincipalDto() {
    }

    // Factory method to map from Users entity
    public static UserPrincipalDto fromEntity(Users user) {
        UserPrincipalDto dto = new UserPrincipalDto();
        dto.id = user.getId();
        dto.name = user.getName();
        dto.email = user.getEmail();
        dto.role = user.getRole();
        dto.emailVerified = user.isEmailVerified();
        dto.banned = user.isBanned();
        dto.passwordChangedAt = user.getPasswordChangedAt();
        return dto;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return null; // Password isn't needed in cache/SecurityContext for JWT authentication
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !banned;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
}