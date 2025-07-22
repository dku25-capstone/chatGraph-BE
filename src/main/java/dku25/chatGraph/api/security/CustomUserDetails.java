package dku25.chatGraph.api.security;

import dku25.chatGraph.api.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    @Getter
    private final User user;
    @Getter
    private final String userId;
    @Getter
    private final String email;
    private final String password;
    private final String role;
    @Getter
    private final String provider;
    @Getter
    private final String providerId;

    // 1. User 엔티티 기반 생성자
    public CustomUserDetails(User user) {
        this.user = user;
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.provider = user.getProvider();
        this.providerId = user.getProviderId();
    }

    // 2. userId/role 기반 생성자 (JWT에서 사용)
    public CustomUserDetails(String userId, String role) {
        this.user = null;
        this.userId = userId;
        this.email = null;
        this.password = null;
        this.role = role;
        this.provider = null;
        this.providerId = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(() -> role);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public String getPassword() { return password; }
    @Override
    public String getUsername() { return email; }
}

