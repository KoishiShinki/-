package io.chronicle.auth;

import io.chronicle.platform.ServiceException;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    public record Identity(long id, String username, String nickname, String role) {}

    public static Identity identity() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof Identity i))
            throw new ServiceException("请先登录", 401);
        return i;
    }

    public static String username() {
        return identity().username();
    }

    public static Long id() {
        return identity().id();
    }

    public static boolean isAdmin() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a != null
                && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
    }
}
