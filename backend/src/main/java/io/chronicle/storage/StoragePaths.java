package io.chronicle.storage;

import io.chronicle.platform.ServiceException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class StoragePaths {
    private static Path root;

    public StoragePaths(@Value("${chronicle.storage-root:./data/uploads}") String path) {
        root = Path.of(path).toAbsolutePath().normalize();
    }

    public static String getProfile() {
        return root.toString();
    }

    public static Path resolve(String url) {
        if (url == null || !url.startsWith("/profile/") || url.contains("\\"))
            throw new ServiceException("图片地址不正确");
        Path path = root.resolve(url.substring(9)).normalize();
        if (!path.startsWith(root)) throw new ServiceException("图片地址不正确");
        return path;
    }
}
