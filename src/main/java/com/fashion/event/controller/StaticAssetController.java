package com.fashion.event.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@RestController
public class StaticAssetController {
    @RequestMapping(path = "/assets/{*assetPath}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Resource> getAsset(@PathVariable String assetPath) {
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(assetPath == null ? "" : assetPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        String normalized = decodedPath.replace('\\', '/');
        // Spring's catch-all path variable includes one leading slash for nested asset paths.
        if (normalized.startsWith("/") && !normalized.startsWith("//")) normalized = normalized.substring(1);
        if (decodedPath.isBlank() || decodedPath.contains("\\") || decodedPath.indexOf('\0') >= 0
                || decodedPath.toLowerCase(java.util.Locale.ROOT).contains("%2e")
                || decodedPath.toLowerCase(java.util.Locale.ROOT).contains("%5c")
                || normalized.startsWith("/") || normalized.startsWith("//")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        for (String segment : normalized.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) throw notFound();
        }
        String extension = normalized.substring(normalized.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        MediaType contentType = switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> throw notFound();
        };

        String classpathPath = "static/assets/" + normalized;
        Resource resource = new ClassPathResource(classpathPath);
        if (!resource.exists() || !resource.isReadable()) {
            Path frontendAsset = Path.of("frontend", "public", "assets").resolve(normalized).normalize();
            Path assetRoot = Path.of("frontend", "public", "assets").toAbsolutePath().normalize();
            if (!frontendAsset.toAbsolutePath().normalize().startsWith(assetRoot)
                    || !Files.isRegularFile(frontendAsset) || !Files.isReadable(frontendAsset)) {
                throw notFound();
            }
            resource = new org.springframework.core.io.FileSystemResource(frontendAsset);
        }

        long lastModified;
        long length;
        String etag;
        try {
            lastModified = resource.lastModified();
            length = resource.contentLength();
            etag = "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    (classpathPath + ":" + length + ":" + lastModified).getBytes(StandardCharsets.UTF_8))) + "\"";
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw notFound();
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header("X-Content-Type-Options", "nosniff")
                .header("ETag", etag)
                .lastModified(lastModified)
                .contentLength(length)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
