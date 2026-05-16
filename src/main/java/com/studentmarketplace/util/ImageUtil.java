package com.studentmarketplace.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for loading, resizing, caching and storing post images.
 */
public final class ImageUtil {
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final String ASSET_DIR = "assets/images";

    private ImageUtil() {
    }

    public static String copyToLocalAssets(File sourceFile) throws IOException {
        if (sourceFile == null) {
            return "";
        }

        Path assetDir = Path.of(ASSET_DIR);
        if (!Files.exists(assetDir)) {
            Files.createDirectories(assetDir);
        }

        String sanitized = sourceFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String targetName = UUID.randomUUID() + "_" + sanitized;
        Path target = assetDir.resolve(targetName);

        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString().replace("\\", "/");
    }

    public static Image loadCached(String imagePath, double width, double height) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String key = imagePath + "|" + width + "|" + height;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        String resolved = resolvePath(imagePath);
        Image image = new Image(resolved, width, height, true, true, true);
        CACHE.put(key, image);
        return image;
    }

    private static String resolvePath(String path) {
        if (path.startsWith("file:")) {
            return path;
        }

        File file = new File(path);
        if (file.exists()) {
            return file.toURI().toString();
        }

        File fallback = new File(path.replace("/", File.separator));
        return fallback.toURI().toString();
    }
}
