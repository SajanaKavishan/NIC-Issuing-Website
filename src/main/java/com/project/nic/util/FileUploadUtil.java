
package com.project.nic.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileUploadUtil {
    private static final long MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final Map<String, String> EXTENSIONS_BY_TYPE = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    public static String saveFile(String uploadDir, MultipartFile file) throws IOException {
        return saveValidatedFile(uploadDir, file, DOCUMENT_TYPES);
    }

    public static String saveDocument(String uploadDir, MultipartFile file) throws IOException {
        return saveValidatedFile(uploadDir, file, DOCUMENT_TYPES);
    }

    public static String saveImage(String uploadDir, MultipartFile file) throws IOException {
        return saveValidatedFile(uploadDir, file, IMAGE_TYPES);
    }

    public static void validateDocument(MultipartFile file) {
        validateFile(file, DOCUMENT_TYPES);
    }

    public static void validateImage(MultipartFile file) {
        validateFile(file, IMAGE_TYPES);
    }

    private static String saveValidatedFile(String uploadDir, MultipartFile file, Set<String> allowedTypes) throws IOException {
        validateFile(file, allowedTypes);

        String contentType = normalizeContentType(file.getContentType());
        String extension = EXTENSIONS_BY_TYPE.get(contentType);
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private static void validateFile(MultipartFile file, Set<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is required");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new IllegalArgumentException("Uploaded file must be 5MB or smaller");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, JPG, PNG");
        }

        String extension = getExtension(file.getOriginalFilename());
        String expectedExtension = EXTENSIONS_BY_TYPE.get(contentType);
        if (extension == null || !extensionMatches(extension, expectedExtension, contentType)) {
            throw new IllegalArgumentException("File extension does not match the uploaded file type");
        }
    }

    private static String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String normalizedName = Paths.get(fileName).getFileName().toString();
        int dotIndex = normalizedName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedName.length() - 1) {
            return null;
        }
        return normalizedName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private static boolean extensionMatches(String extension, String expectedExtension, String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return ".jpg".equals(extension) || ".jpeg".equals(extension);
        }
        return expectedExtension != null && expectedExtension.equals(extension);
    }
}
