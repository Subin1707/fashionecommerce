package com.fashion.event.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CampaignImageStorageService {
    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final int MAX_IMAGE_EDGE = 10000;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;
    private final Path eventDirectory;
    private final Path avatarDirectory;

    public CampaignImageStorageService(@Value("${fashion.upload.directory:uploads}") String uploadDirectory) {
        this.eventDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("events").normalize();
        this.avatarDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("avatars").normalize();
    }

    public String store(MultipartFile file) {
        return store(file, eventDirectory, "events", "ảnh banner");
    }

    public String storeAvatar(MultipartFile file) {
        return store(file, avatarDirectory, "avatars", "ảnh đại diện");
    }

    private String store(MultipartFile file, Path directory, String folder, String label) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn " + label + " trước khi tải lên.");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException(label + " tối đa 8 MB.");

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPEG hoặc PNG.");
        };

        try {
            byte[] bytes = file.getBytes();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if ((extension.equals("jpg") && !format.equals("jpeg") && !format.equals("jpg"))
                            || (extension.equals("png") && !format.equals("png"))) {
                        throw new IllegalArgumentException("Định dạng ảnh không khớp với loại tệp đã chọn.");
                    }
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 2 || height < 2 || width > MAX_IMAGE_EDGE || height > MAX_IMAGE_EDGE
                            || (long) width * height > MAX_IMAGE_PIXELS) {
                        throw new IllegalArgumentException("Kích thước " + label + " vượt quá giới hạn cho phép.");
                    }
                    BufferedImage image = reader.read(0);
                    if (image == null) throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ.");
                } finally {
                    reader.dispose();
                }
            }

            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "." + extension;
            Path destination = directory.resolve(filename).normalize();
            if (!destination.startsWith(directory)) throw new IllegalArgumentException("Đường dẫn ảnh không hợp lệ.");
            Files.write(destination, bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể lưu " + label + ".", exception);
        }
    }

    public void deleteIfManaged(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/events/")) return;
        String filename = imageUrl.substring("/uploads/events/".length());
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) return;
        try {
            Path file = eventDirectory.resolve(filename).normalize();
            if (file.startsWith(eventDirectory)) Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Stale files do not affect campaign availability.
        }
    }

    public Path getEventDirectory() {
        return eventDirectory;
    }

    public Path getAvatarDirectory() {
        return avatarDirectory;
    }
}
