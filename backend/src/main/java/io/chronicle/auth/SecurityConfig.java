package io.chronicle.auth;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            AuthService auth,
            @org.springframework.beans.factory.annotation.Value(
                            "${chronicle.app-origin:http://localhost:5173}")
                    String origin)
            throws Exception {
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/app/auth/login",
                                                "/app/auth/register",
                                                "/public/**",
                                                "/profile/**",
                                                "/health",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/timeline/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (r, s, x) -> {
                                                    s.setStatus(401);
                                                    s.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    s.getWriter()
                                                            .write(
                                                                    "{\"code\":401,\"msg\":\"请先登录\"}");
                                                })
                                        .accessDeniedHandler(
                                                (r, s, x) -> {
                                                    s.setStatus(403);
                                                    s.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    s.getWriter()
                                                            .write(
                                                                    "{\"code\":403,\"msg\":\"没有访问权限\"}");
                                                }))
                .addFilterBefore(
                        new SessionFilter(auth, origin), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
