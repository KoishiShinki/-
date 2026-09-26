package io.chronicle.storage;

import io.chronicle.platform.ServiceException;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

@org.springframework.stereotype.Service
public final class ImageUploads {
    private final MediaCatalog catalog;

    public ImageUploads(MediaCatalog catalog) {
        this.catalog = catalog;
    }

    public String upload(String folder, MultipartFile file, String[] ignored, boolean rename)
            throws Exception {
        if (file == null || file.isEmpty() || file.getSize() > 20 * 1024 * 1024)
            throw new ServiceException("请上传 20MB 以内的图片");
        Path root = Path.of(StoragePaths.getProfile()), dir = Path.of(folder).normalize();
        if (!dir.startsWith(root)) throw new ServiceException("上传路径不正确");
        String format;
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("请上传有效 PNG、JPEG 或 GIF 图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000)
                    throw new ServiceException("图片像素过大");
                format = reader.getFormatName().toLowerCase();
                if (!java.util.List.of("png", "jpeg", "jpg", "gif").contains(format))
                    throw new ServiceException("不支持此图片格式");
            } finally {
                reader.dispose();
            }
        }
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + "." + format;
        Path target = dir.resolve(name);
        try (var in = file.getInputStream()) {
            Files.copy(in, target);
        }
        String url = "/profile/" + root.relativize(target).toString().replace('\\', '/');
        catalog.register(url);
        return url;
    }
}
