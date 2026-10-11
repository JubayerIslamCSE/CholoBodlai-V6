package com.jubayer.cholobodlai.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Signed server-side Cloudinary Upload API integration, without browser-exposed credentials.
 * Truck photos are public CDN images; registration documents use authenticated delivery.
 * Cloudinary REST Upload API supports HTTP Basic authentication on server-side uploads.
 */
public final class CloudinaryMediaGateway {
    private static final Pattern PUBLIC_ID_FIELD =
            Pattern.compile("\\\"public_id\\\"\\s*:\\s*\\\"([^\\\"\\\\]+)\\\"");
    private static final Pattern REFERENCE = Pattern.compile(
            "^cld:(image|raw):(upload|authenticated):([A-Za-z0-9_-]+):(jpg|png|pdf)$");
    private static final Pattern OWN_PUBLIC_ID = Pattern.compile(
            "^cholobodlai/(?:truck-photos|truck-documents)/[a-f0-9-]{36}(?:\\.pdf)?$");

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final HttpClient client;
    private final URI apiOrigin;

    public CloudinaryMediaGateway(String cloudName, String apiKey, String apiSecret) {
        this(cloudName, apiKey, apiSecret, URI.create("https://api.cloudinary.com"));
    }

    // Package-private to allow deterministic HTTP fake-server tests without real credentials.
    CloudinaryMediaGateway(String cloudName, String apiKey, String apiSecret, URI apiOrigin) {
        if (cloudName == null || !cloudName.matches("[a-zA-Z0-9_-]{1,80}")
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalArgumentException("Cloudinary cloud name, API key and API secret are required");
        }
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiOrigin = Objects.requireNonNull(apiOrigin);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public boolean isReference(String reference) {
        return reference != null && reference.startsWith("cld:");
    }

    public String uploadPhoto(byte[] bytes, String format) {
        if (!format.equals("jpg") && !format.equals("png")) {
            throw new IllegalArgumentException("Photos must be JPEG or PNG");
        }
        String publicId = "cholobodlai/truck-photos/" + UUID.randomUUID();
        upload(bytes, "image", "upload", publicId, format);
        return encodeReference("image", "upload", publicId, format);
    }

    public String uploadDocument(byte[] bytes, String format) {
        if (!format.equals("jpg") && !format.equals("png") && !format.equals("pdf")) {
            throw new IllegalArgumentException("Documents must be JPEG, PNG or PDF");
        }
        String resource = format.equals("pdf") ? "raw" : "image";
        String publicId = "cholobodlai/truck-documents/" + UUID.randomUUID()
                + (resource.equals("raw") ? ".pdf" : "");
        upload(bytes, resource, "authenticated", publicId, format);
        return encodeReference(resource, "authenticated", publicId, format);
    }

    private void upload(byte[] bytes, String resource, String type, String publicId, String format) {
        String boundary = "----CholoBodlai-" + UUID.randomUUID().toString().replace("-", "");
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream(bytes.length + 1500);
            field(body, boundary, "public_id", publicId);
            field(body, boundary, "type", type);
            field(body, boundary, "overwrite", "false");
            String mime = switch (format) {
                case "pdf" -> "application/pdf";
                case "png" -> "image/png";
                default -> "image/jpeg";
            };
            write(body, "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"truck." + format + "\"\r\n"
                    + "Content-Type: " + mime + "\r\n\r\n");
            body.write(bytes);
            write(body, "\r\n--" + boundary + "--\r\n");
            URI destination = apiOrigin.resolve("/v1_1/" + cloudName + "/" + resource + "/upload");
            HttpRequest request = HttpRequest.newBuilder(destination)
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            (apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Never log returned response: it can contain account identifiers or sensitive metadata.
                throw new IllegalStateException("Cloudinary rejected the upload (HTTP " + response.statusCode() + ")");
            }
            Matcher match = PUBLIC_ID_FIELD.matcher(response.body());
            if (!match.find() || !publicId.equals(match.group(1))) {
                throw new IllegalStateException("Cloudinary response did not confirm the uploaded asset");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to reach Cloudinary; please try the upload again", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cloudinary upload was interrupted", ex);
        }
    }

    public String photoUrl(String reference) {
        Reference ref = parse(reference);
        if (!ref.resource.equals("image") || !ref.type.equals("upload")
                || !ref.publicId.startsWith("cholobodlai/truck-photos/")
                || !(ref.format.equals("jpg") || ref.format.equals("png"))) {
            throw new IllegalArgumentException("Not a public truck photo");
        }
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/"
                + ref.publicId + "." + ref.format;
    }

    /**
     * Returns an expiring signed Cloudinary download URL; call ONLY after admin authorization.
     * The Cloudinary authenticated delivery type also protects derived versions.
     */
    public String privateDocumentUrl(String reference) {
        Reference ref = parse(reference);
        if (!ref.type.equals("authenticated") || !ref.publicId.startsWith("cholobodlai/truck-documents/")) {
            throw new IllegalArgumentException("Not a private registration document");
        }
        long now = Instant.now().getEpochSecond();
        Map<String, String> values = new TreeMap<>();
        values.put("public_id", ref.publicId);
        values.put("format", ref.format);
        values.put("type", "authenticated");
        values.put("attachment", "true");
        values.put("expires_at", Long.toString(now + 60));
        values.put("timestamp", Long.toString(now));
        String signature = signature(values);
        values.put("signature", signature);
        values.put("api_key", apiKey);
        StringBuilder url = new StringBuilder("https://api.cloudinary.com/v1_1/")
                .append(cloudName).append('/').append(ref.resource).append("/download?");
        for (Map.Entry<String, String> pair : values.entrySet()) {
            if (url.charAt(url.length() - 1) != '?') url.append('&');
            url.append(encode(pair.getKey())).append('=').append(encode(pair.getValue()));
        }
        return url.toString();
    }

    /** A deterministic signature helper for API calls; map must not include api_key/signature. */
    String signature(Map<String, String> parameters) {
        try {
            StringBuilder canonical = new StringBuilder();
            for (Map.Entry<String, String> pair : new TreeMap<>(parameters).entrySet()) {
                if (pair.getKey().equals("api_key") || pair.getKey().equals("signature")) continue;
                if (canonical.length() > 0) canonical.append('&');
                canonical.append(pair.getKey()).append('=').append(pair.getValue());
            }
            canonical.append(apiSecret);
            byte[] sha = MessageDigest.getInstance("SHA-1")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(sha);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void deleteQuietly(String reference) {
        if (!isReference(reference)) return;
        try {
            Reference ref = parse(reference);
            long timestamp = Instant.now().getEpochSecond();
            Map<String, String> signed = new TreeMap<>();
            signed.put("public_id", ref.publicId);
            signed.put("type", ref.type);
            signed.put("invalidate", "true");
            signed.put("timestamp", Long.toString(timestamp));
            String signature = signature(signed);
            signed.put("api_key", apiKey);
            signed.put("signature", signature);
            StringBuilder data = new StringBuilder();
            for (var entry : signed.entrySet()) {
                if (data.length() > 0) data.append('&');
                data.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            }
            HttpRequest request = HttpRequest.newBuilder(apiOrigin.resolve(
                            "/v1_1/" + cloudName + "/" + ref.resource + "/destroy"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Best-effort cleanup; MongoDB record remains source of truth.
        }
    }

    private Reference parse(String encoded) {
        Matcher match = REFERENCE.matcher(Objects.toString(encoded, ""));
        if (!match.matches()) throw new IllegalArgumentException("Invalid Cloudinary asset reference");
        String id;
        try {
            id = new String(Base64.getUrlDecoder().decode(match.group(3)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Malformed Cloudinary asset reference");
        }
        if (!OWN_PUBLIC_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Asset is outside CholoBodlai upload folders");
        }
        return new Reference(match.group(1), match.group(2), id, match.group(4));
    }

    private static String encodeReference(String resource, String type, String id, String format) {
        return "cld:" + resource + ":" + type + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(StandardCharsets.UTF_8))
                + ":" + format;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void write(ByteArrayOutputStream target, String text) throws IOException {
        target.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void field(ByteArrayOutputStream target, String boundary, String name, String value)
            throws IOException {
        write(target, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"\r\n\r\n" + value + "\r\n");
    }

    private record Reference(String resource, String type, String publicId, String format) { }
}
