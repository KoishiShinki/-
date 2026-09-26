package io.chronicle.storage;

import io.chronicle.auth.CurrentUser;
import io.chronicle.platform.ServiceException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MediaCatalog {
    private final JdbcTemplate db;

    public MediaCatalog(JdbcTemplate db) {
        this.db = db;
    }

    public void register(String url) {
        db.update("insert into app_media(url,owner_user_id) values(?,?)", url, CurrentUser.id());
    }

    public void validate(String url) {
        if (url == null || url.isBlank()) return;
        if (url.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                if (uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException();
                return;
            } catch (Exception e) {
                throw new ServiceException("图片地址无效");
            }
        }
        StoragePaths.resolve(url);
        Integer owned =
                db.queryForObject(
                        "select count(*) from app_media where url=? and owner_user_id=?",
                        Integer.class,
                        url,
                        CurrentUser.id());
        if (owned == 0 && !CurrentUser.isAdmin()) throw new ServiceException("只能使用自己上传的图片", 403);
    }

    public boolean exists(String url) {
        return db.queryForObject("select count(*) from app_media where url=?", Integer.class, url)
                > 0;
    }

    public boolean owned(String url) {
        return db.queryForObject(
                        "select count(*) from app_media where url=? and owner_user_id=?",
                        Integer.class,
                        url,
                        CurrentUser.id())
                > 0;
    }
}
