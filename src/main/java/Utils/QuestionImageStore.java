package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

public class QuestionImageStore {

    private static final String DIR_NAME = "question_images";
    private static final String MAP_FILE_NAME = "map.properties";

    private final Path imagesDir;
    private final Path mapPath;

    public QuestionImageStore() {
        Path baseDir = Path.of(System.getProperty("user.home"), ".careerlink");
        this.imagesDir = baseDir.resolve(DIR_NAME);
        this.mapPath = imagesDir.resolve(MAP_FILE_NAME);
    }

    public Path getImagesDir() {
        return imagesDir;
    }

    public Path saveImportedImage(int questionId, Path sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");
        ensureStorageReady();
        String fileName = buildFileName(questionId);
        Path target = imagesDir.resolve(fileName);

        try {
            Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de sauvegarder l'image importee.", e);
        }
    }

    public Path saveGeneratedPng(int questionId, byte[] pngBytes) {
        ensureStorageReady();
        String fileName = buildFileName(questionId);
        Path target = imagesDir.resolve(fileName);
        try {
            Files.write(target, pngBytes);
            return target.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de sauvegarder l'image generee.", e);
        }
    }

    public void saveMapping(int questionId, Path imagePath) {
        ensureStorageReady();
        Properties properties = loadMap();
        properties.setProperty(String.valueOf(questionId), imagePath.toAbsolutePath().normalize().toString());
        persistMap(properties);
    }

    public void removeMapping(int questionId) {
        ensureStorageReady();
        Properties properties = loadMap();
        String key = String.valueOf(questionId);
        String oldPath = properties.getProperty(key);
        properties.remove(key);
        persistMap(properties);
        if (oldPath != null && !oldPath.isBlank()) {
            try {
                Files.deleteIfExists(Path.of(oldPath));
            } catch (IOException ignored) {
                // Non-blocking cleanup.
            }
        }
    }

    public Path resolveImagePath(int questionId, String reponseValue) {
        ensureStorageReady();
        Properties properties = loadMap();
        String mapped = properties.getProperty(String.valueOf(questionId));
        Path mappedPath = toExistingPath(mapped);
        if (mappedPath != null) {
            return mappedPath;
        }

        Path fromReponse = toExistingPath(reponseValue);
        if (fromReponse != null) {
            return fromReponse;
        }
        return null;
    }

    private void ensureStorageReady() {
        try {
            Files.createDirectories(imagesDir);
            if (!Files.exists(mapPath)) {
                Files.createFile(mapPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le stockage d'images.", e);
        }
    }

    private Properties loadMap() {
        Properties properties = new Properties();
        if (!Files.exists(mapPath)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(mapPath)) {
            properties.load(in);
        } catch (IOException ignored) {
            // Keep empty map as fallback.
        }
        return properties;
    }

    private void persistMap(Properties properties) {
        try (OutputStream out = Files.newOutputStream(mapPath)) {
            properties.store(out, "Question image mapping");
        } catch (IOException e) {
            throw new RuntimeException("Impossible de sauvegarder map.properties.", e);
        }
    }

    private Path toExistingPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(value).toAbsolutePath().normalize();
            return Files.exists(path) ? path : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildFileName(int questionId) {
        long now = System.currentTimeMillis();
        return "q_" + questionId + "_" + now + ".png";
    }
}
