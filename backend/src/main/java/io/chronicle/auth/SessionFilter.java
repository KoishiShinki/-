package io.chronicle.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class SessionFilter extends OncePerRequestFilter {
    private final AuthService auth;
    private final String appOrigin;

    public SessionFilter(AuthService auth, String appOrigin) {
        this.auth = auth;
        this.appOrigin = appOrigin;
    }

    public static String token(HttpServletRequest r) {
        String header = r.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        if (r.getCookies() != null)
            for (var c : r.getCookies())
                if (c.getName().equals("chronicle_session")) return c.getValue();
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) && origin != null) {
            String expected = request.getScheme() + "://" + request.getHeader("Host");
            if (!expected.equals(origin) && !appOrigin.equals(origin)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"跨来源请求被拒绝\"}");
                return;
            }
        }
        var identity = auth.session(token(request));
        if (identity != null)
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    identity,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + identity.role()))));
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
