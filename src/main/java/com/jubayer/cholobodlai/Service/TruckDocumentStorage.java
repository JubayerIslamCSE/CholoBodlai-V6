package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * V6 storage compatibility layer. Cloudinary is used on Render; local development can
 * intentionally fall back to disk. Old UUID filenames remain readable on the old PC.
 * NEVER save new public-deployment uploads to Render's ephemeral filesystem.
 */
@Service
public class TruckDocumentStorage {
    private final Path localRoot;
    private final CloudinaryMediaGateway cloudinary;

    public TruckDocumentStorage(
            @Value("${app.documents.directory:./uploads/truck-documents}") String directory,
            @Value("${app.media.backend:auto}") String backend,
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloud,
            @Value("${CLOUDINARY_API_KEY:}") String key,
            @Value("${CLOUDINARY_API_SECRET:}") String secret,
            @Value("${RENDER:}") String render) {
        this.localRoot = Path.of(directory).toAbsolutePath().normalize();
        String mode = backend == null ? "auto" : backend.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("auto", "local", "cloudinary").contains(mode)) {
            throw new IllegalStateException("APP_MEDIA_BACKEND must be auto, local or cloudinary");
        }
        boolean anyCloudConfig = !cloud.isBlank() || !key.isBlank() || !secret.isBlank();
        boolean allCloudConfig = !cloud.isBlank() && !key.isBlank() && !secret.isBlank();
        if (anyCloudConfig && !allCloudConfig) {
            throw new IllegalStateException("Set all three CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET");
        }
        if ("cloudinary".equals(mode) && !allCloudConfig) {
            throw new IllegalStateException("Cloudinary storage selected; set all three Cloudinary credentials");
        }
        boolean isRender = render != null && !render.isBlank();
        if (isRender && "local".equals(mode)) {
            throw new IllegalStateException("Local upload storage is not persistent on Render. Select Cloudinary");
        }
        if (isRender && !allCloudConfig) {
            throw new IllegalStateException("Configure Cloudinary before deploying to Render; uploads must be persistent");
        }
        this.cloudinary = allCloudConfig && !"local".equals(mode)
                ? new CloudinaryMediaGateway(cloud, key, secret)
                : null;
    }

    public boolean usingCloudinary() { return cloudinary != null; }
    public boolean isCloudinaryReference(String reference) {
        return reference != null && reference.startsWith("cld:");
    }

    /** Stores an authenticated private JPG, PNG or PDF registration document. */
    public String store(MultipartFile file) {
        return storeInternal(file, false);
    }

    /** Stores a public vehicle image. PDF uploads are disallowed. */
    public String storePhoto(MultipartFile file) {
        return storeInternal(file, true);
    }

    private String storeInternal(MultipartFile file, boolean isPhoto) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() <= 0 || file.getSize() > 5_000_000) {
            throw new BusinessRuleException("Upload a file smaller than 5 MB");
        }
        String original = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "";
        if (ext.equals("jpeg")) ext = "jpg";
        if (!Set.of("jpg", "png", "pdf").contains(ext) || (isPhoto && ext.equals("pdf"))) {
            throw new BusinessRuleException(isPhoto
                    ? "Vehicle photo must be JPG or PNG" : "Truck document must be JPG, PNG or PDF");
        }
        String mime = Objects.toString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (!mime.equals(switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            default -> "image/jpeg";
        })) throw new BusinessRuleException("File type and extension do not match");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > 5_000_000 || !hasValidSignature(bytes, ext)) {
                throw new BusinessRuleException("The uploaded file is invalid or too large");
            }
            if (cloudinary != null) {
                try {
                    return isPhoto ? cloudinary.uploadPhoto(bytes, ext)
                            : cloudinary.uploadDocument(bytes, ext);
                } catch (RuntimeException ex) {
                    throw new BusinessRuleException("Cloudinary could not save the upload. Check its settings and try again");
                }
            }
            Files.createDirectories(localRoot);
            String id = UUID.randomUUID() + "." + ext;
            Files.write(localRoot.resolve(id), bytes, StandardOpenOption.CREATE_NEW);
            return id;
        } catch (IOException ex) {
            throw new BusinessRuleException("Upload could not be saved. Try again");
        }
    }

    static boolean hasValidSignature(byte[] bytes, String ext) {
        if (bytes == null) return false;
        return switch (ext) {
            case "pdf" -> bytes.length >= 5 && bytes[0]=='%' && bytes[1]=='P' && bytes[2]=='D'
                    && bytes[3]=='F' && bytes[4]=='-';
            case "png" -> bytes.length >= 8 && (bytes[0]&255)==137 && bytes[1]==80
                    && bytes[2]==78 && bytes[3]==71 && bytes[4]==13 && bytes[5]==10
                    && bytes[6]==26 && bytes[7]==10;
            case "jpg" -> bytes.length >= 3 && (bytes[0]&255)==255
                    && (bytes[1]&255)==216 && (bytes[2]&255)==255;
            default -> false;
        };
    }

    public String publicPhotoUrl(String reference) {
        if (cloudinary == null || !isCloudinaryReference(reference)) {
            throw new BusinessRuleException("Cloudinary image is not available");
        }
        try { return cloudinary.photoUrl(reference); }
        catch (IllegalArgumentException ex) { throw new BusinessRuleException("Vehicle image link is invalid"); }
    }

    public String privateDocumentUrl(String reference) {
        if (cloudinary == null || !isCloudinaryReference(reference)) {
            throw new BusinessRuleException("Cloudinary document is not available");
        }
        try { return cloudinary.privateDocumentUrl(reference); }
        catch (IllegalArgumentException ex) { throw new BusinessRuleException("Private document link is invalid"); }
    }

    /** Legacy V5 document/photo reader (local files only; not available after migrating to Render). */
    public Resource load(String id) {
        if (id == null || !id.matches("[a-fA-F0-9-]{36}\\.(pdf|png|jpg|jpeg)")) {
            throw new BusinessRuleException("Legacy file is missing; ask the owner to re-upload to Cloudinary");
        }
        Path safe = localRoot.resolve(id).normalize();
        if (!safe.startsWith(localRoot)) throw new BusinessRuleException("Invalid file location");
        try {
            Resource resource = new UrlResource(safe.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessRuleException("This old file was stored on another computer. Please re-upload it");
            }
            return resource;
        } catch (java.net.MalformedURLException ex) {
            throw new BusinessRuleException("Stored file address is invalid");
        }
    }

    public void deleteQuietly(String id) {
        if (id == null) return;
        if (isCloudinaryReference(id)) {
            if (cloudinary != null) cloudinary.deleteQuietly(id);
            return;
        }
        if (!id.matches("[a-fA-F0-9-]{36}\\.(pdf|png|jpg|jpeg)")) return;
        try { Files.deleteIfExists(localRoot.resolve(id)); }
        catch (IOException ignored) { }
    }
}
