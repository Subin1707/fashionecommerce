package com.fashion.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashion.event.service.CampaignImageStorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class CampaignImageStorageServiceTest {
    @TempDir Path tempDir;

    @Test
    void storesValidPngAndDeletesManagedImage() throws Exception {
        CampaignImageStorageService storage = new CampaignImageStorageService(tempDir.toString());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), "png", output);
        MockMultipartFile image = new MockMultipartFile("file", "campaign.png", "image/png", output.toByteArray());

        String url = storage.store(image);
        Path saved = tempDir.resolve("events").resolve(url.substring(url.lastIndexOf('/') + 1));
        assertThat(Files.exists(saved)).isTrue();
        storage.deleteIfManaged(url);
        assertThat(Files.exists(saved)).isFalse();
    }

    @Test
    void rejectsInvalidImageBytesAndUnsupportedTypes() {
        CampaignImageStorageService storage = new CampaignImageStorageService(tempDir.toString());
        MockMultipartFile fakePng = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        MockMultipartFile svg = new MockMultipartFile("file", "banner.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> storage.store(fakePng)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store(svg)).isInstanceOf(IllegalArgumentException.class);
    }
}
