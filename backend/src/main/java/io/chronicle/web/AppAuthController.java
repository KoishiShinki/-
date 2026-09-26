package io.chronicle.web;

import io.chronicle.auth.*;
import io.chronicle.platform.*;
import io.chronicle.storage.*;
import io.chronicle.timeline.domain.*;
import io.chronicle.timeline.domain.dto.*;
import io.chronicle.timeline.service.ITimelineService;

import jakarta.servlet.http.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/app/auth")
public class AppAuthController extends ApiController {
    private final ImageUploads imageUploads;
    private final AuthService auth;
    private final ITimelineService timeline;
    private final boolean secure;

    public AppAuthController(
            AuthService auth,
            ITimelineService timeline,
            ImageUploads imageUploads,
            @Value("${chronicle.secure-cookie:false}") boolean secure) {
        this.imageUploads = imageUploads;
        this.auth = auth;
        this.timeline = timeline;
        this.secure = secure;
    }

    @PostMapping("/register")
    public ApiResponse register(@RequestBody AppRegisterRequest r) {
        if (r.getPassword() == null || !r.getPassword().equals(r.getConfirmPassword()))
            throw new ServiceException("两次密码不一致");
        var u = auth.register(r.getUsername(), r.getNickname(), r.getPassword(), "USER");
        ensure(u);
        return success();
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody AppLoginRequest r, HttpServletResponse response) {
        var u = auth.verify(r.getUsername(), r.getPassword());
        String token = auth.issue(u);
        cookie(response, token, 604800);
        var result = identity(u);
        result.put("token", token);
        return result;
    }

    @PostMapping("/logout")
    public ApiResponse logout(HttpServletRequest request, HttpServletResponse response) {
        auth.revoke(SessionFilter.token(request));
        cookie(response, "", 0);
        return success();
    }

    @GetMapping("/profile")
    public ApiResponse profile() {
        return identity(CurrentUser.identity());
    }

    @PutMapping("/profile")
    public ApiResponse update(@RequestBody TlUserProfile p) {
        p.setUserId(getUserId());
        return toAjax(timeline.saveUserProfile(p));
    }

    @PostMapping("/avatar")
    public ApiResponse avatar(@RequestParam("file") MultipartFile f) throws Exception {
        var p = ensure(CurrentUser.identity());
        String path =
                imageUploads.upload(
                        StoragePaths.getProfile() + "/avatar/" + getUserId(),
                        f,
                        ImageTypes.IMAGE_EXTENSION,
                        true);
        p.setAvatarUrl(path);
        timeline.saveUserProfile(p);
        return success(path);
    }

    private TlUserProfile ensure(CurrentUser.Identity u) {
        var p = timeline.selectUserProfileByUserId(u.id());
        if (p == null) {
            p = new TlUserProfile();
            p.setUserId(u.id());
            p.setNickname(u.nickname());
            p.setCreatorTitle("世界线创作者");
            timeline.saveUserProfile(p);
        }
        return p;
    }

    private ApiResponse identity(CurrentUser.Identity u) {
        var r = success();
        var p = ensure(u);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", u.id());
        user.put("username", u.username());
        user.put("nickname", p.getNickname());
        user.put("avatar", p.getAvatarUrl());
        user.put("roles", List.of(u.role()));
        r.put("user", user);
        r.put("profile", p);
        return r;
    }

    private void cookie(HttpServletResponse r, String token, long seconds) {
        r.addHeader(
                "Set-Cookie",
                ResponseCookie.from("chronicle_session", token)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(seconds)
                        .build()
                        .toString());
    }
}
