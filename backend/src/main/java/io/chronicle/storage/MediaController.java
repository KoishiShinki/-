package io.chronicle.storage;

import io.chronicle.auth.CurrentUser;
import io.chronicle.platform.ServiceException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;

@RestController
public class MediaController {
    private final JdbcTemplate db;
    private final MediaCatalog catalog;

    public MediaController(JdbcTemplate db, MediaCatalog catalog) {
        this.db = db;
        this.catalog = catalog;
    }

    @GetMapping("/profile/**")
    public ResponseEntity<FileSystemResource> image(HttpServletRequest req) throws Exception {
        String url = req.getRequestURI();
        Path path = StoragePaths.resolve(url);
        if (!allowed(url) || !Files.isRegularFile(path) || Files.isSymbolicLink(path))
            throw new ServiceException("图片不存在或无权访问", 404);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(
                        MediaType.parseMediaType(
                                java.util.Objects.requireNonNullElse(
                                        Files.probeContentType(path), "application/octet-stream")))
                .body(new FileSystemResource(path));
    }

    private boolean count(String sql, Object... args) {
        return db.queryForObject(sql, Integer.class, args) > 0;
    }

    private boolean allowed(String url) {
        if (!catalog.exists(url)) return false;
        if (count("select count(*) from tl_worldline where cover_url=? and visibility='1'", url))
            return true;
        if (count(
                "select count(*) from tl_illustration i join tl_worldline w on"
                    + " w.worldline_id=i.worldline_id where i.image_url=? and i.generate_status='4'"
                    + " and w.visibility='1'",
                url)) return true;
        if (count(
                "select count(*) from tl_user_profile where avatar_url=? or homepage_cover_url=?",
                url,
                url)) return true;
        String username;
        try {
            username = CurrentUser.username();
        } catch (ServiceException e) {
            return false;
        }
        if (CurrentUser.isAdmin() || catalog.owned(url)) return true;
        if (count(
                "select count(*) from tl_worldline where cover_url=? and create_by=?",
                url,
                username)) return true;
        for (String[] t :
                new String[][] {
                    {"tl_screenshot", "image_url"},
                    {"tl_illustration", "image_url"},
                    {"tl_character", "portrait_url"}
                })
            if (count(
                    "select count(*) from "
                            + t[0]
                            + " x join tl_worldline w on w.worldline_id=x.worldline_id where x."
                            + t[1]
                            + "=? and w.create_by=?",
                    url,
                    username)) return true;
        return false;
    }
}
