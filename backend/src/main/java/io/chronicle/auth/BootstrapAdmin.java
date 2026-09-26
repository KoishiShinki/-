package io.chronicle.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final AuthService auth;
    private final String username, password;

    public BootstrapAdmin(
            AuthService auth,
            @Value("${chronicle.admin-username:}") String username,
            @Value("${chronicle.admin-password:}") String password) {
        this.auth = auth;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!username.isBlank() && !password.isBlank() && auth.find(username) == null)
            auth.register(username, "Administrator", password, "ADMIN");
    }
}
