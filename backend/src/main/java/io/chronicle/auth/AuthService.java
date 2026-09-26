package io.chronicle.auth;

import io.chronicle.platform.ServiceException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public AuthService(JdbcTemplate db) {
        this.db = db;
    }

    public CurrentUser.Identity find(String username) {
        return db
                .query(
                        "select user_id,user_name,nick_name,role from app_user where user_name=?"
                            + " and enabled=true",
                        (r, n) ->
                                new CurrentUser.Identity(
                                        r.getLong(1),
                                        r.getString(2),
                                        r.getString(3),
                                        r.getString(4)),
                        username)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void validate(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,40}"))
            throw new ServiceException("用户名需为 3–40 位字母、数字或下划线");
        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ServiceException("密码需至少 12 个字符，且 UTF-8 长度不超过 72 字节");
    }

    @Transactional
    public CurrentUser.Identity register(
            String username, String nickname, String password, String role) {
        validate(username, password);
        if (nickname == null || nickname.isBlank()) nickname = username;
        if (nickname.length() > 80) throw new ServiceException("昵称不能超过 80 个字符");
        db.update(
                "insert into app_user(user_name,nick_name,password_hash,role) values(?,?,?,?)",
                username,
                nickname,
                encoder.encode(password),
                role);
        return find(username);
    }

    public CurrentUser.Identity verify(String username, String password) {
        var hashes =
                db.queryForList(
                        "select password_hash from app_user where user_name=? and enabled=true",
                        String.class,
                        username);
        if (hashes.isEmpty() || password == null || !encoder.matches(password, hashes.get(0)))
            throw new ServiceException("用户名或密码错误", 401);
        return find(username);
    }

    public String issue(CurrentUser.Identity user) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("delete from app_session where expires_at<?", Timestamp.from(Instant.now()));
        db.update(
                "insert into app_session(token_hash,user_id,expires_at) values(?,?,?)",
                hash(token),
                user.id(),
                Timestamp.from(Instant.now().plusSeconds(604800)));
        return token;
    }

    public CurrentUser.Identity session(String token) {
        if (token == null || token.length() > 128) return null;
        return db
                .query(
                        "select u.user_id,u.user_name,u.nick_name,u.role from app_session s join"
                            + " app_user u on u.user_id=s.user_id where token_hash=? and"
                            + " expires_at>? and u.enabled=true",
                        (r, n) ->
                                new CurrentUser.Identity(
                                        r.getLong(1),
                                        r.getString(2),
                                        r.getString(3),
                                        r.getString(4)),
                        hash(token),
                        Timestamp.from(Instant.now()))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void revoke(String token) {
        if (token != null) db.update("delete from app_session where token_hash=?", hash(token));
    }

    private String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
