package com.project.nic.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadUtilTests {

    @TempDir
    private Path tempDir;

    @Test
    void validateDocumentAcceptsPdfJpgAndPngDocuments() {
        assertThatCode(() -> FileUploadUtil.validateDocument(file("birth.pdf", "application/pdf")))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileUploadUtil.validateDocument(file("scan.jpg", "image/jpeg")))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileUploadUtil.validateDocument(file("scan.png", "image/png")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateImageAcceptsJpgJpegAndPngButRejectsPdf() {
        assertThatCode(() -> FileUploadUtil.validateImage(file("photo.jpg", "image/jpeg")))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileUploadUtil.validateImage(file("photo.jpeg", "image/jpeg")))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileUploadUtil.validateImage(file("photo.png", "image/png")))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> FileUploadUtil.validateImage(file("photo.pdf", "application/pdf")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file type. Allowed types: PDF, JPG, PNG");
    }

    @Test
    void validateFileRejectsMissingEmptyOrOversizedUploads() {
        assertThatThrownBy(() -> FileUploadUtil.validateDocument(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is required");

        assertThatThrownBy(() -> FileUploadUtil.validateDocument(
                new MockMultipartFile("document", "empty.pdf", "application/pdf", new byte[0])
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is required");

        assertThatThrownBy(() -> FileUploadUtil.validateDocument(
                new MockMultipartFile("document", "large.pdf", "application/pdf", new byte[(5 * 1024 * 1024) + 1])
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file must be 5MB or smaller");
    }

    @Test
    void validateFileRejectsInvalidTypeAndMismatchedExtension() {
        assertThatThrownBy(() -> FileUploadUtil.validateDocument(file("notes.txt", "text/plain")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file type. Allowed types: PDF, JPG, PNG");

        assertThatThrownBy(() -> FileUploadUtil.validateDocument(file("birth.jpg", "application/pdf")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File extension does not match the uploaded file type");

        assertThatThrownBy(() -> FileUploadUtil.validateDocument(file("birth", "application/pdf")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File extension does not match the uploaded file type");
    }

    @Test
    void saveDocumentWritesValidatedFileInsideUploadDirectory() throws Exception {
        String savedPath = FileUploadUtil.saveDocument(tempDir.toString(), file("birth.pdf", "application/pdf"));
        Path savedFile = Path.of(savedPath);

        assertThat(savedFile).exists();
        assertThat(savedFile.toAbsolutePath().normalize()).startsWith(tempDir.toAbsolutePath().normalize());
        assertThat(savedFile.getFileName().toString()).endsWith(".pdf");
        assertThat(Files.readString(savedFile)).isEqualTo("file-content");
    }

    @Test
    void saveImageUsesExtensionForNormalizedContentType() throws Exception {
        String savedPath = FileUploadUtil.saveImage(tempDir.toString(), file("photo.JPEG", " IMAGE/JPEG "));

        assertThat(Path.of(savedPath)).exists();
        assertThat(savedPath).endsWith(".jpg");
    }

    private MockMultipartFile file(String originalFilename, String contentType) {
        return new MockMultipartFile("file", originalFilename, contentType, "file-content".getBytes());
    }
}
