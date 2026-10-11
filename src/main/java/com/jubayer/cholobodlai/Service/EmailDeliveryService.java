package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.Model.MailPreview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Preferred public email delivery: Resend HTTPS API (compatible with Render Free).
 * SMTP remains available for local/paid hosts; local-only preview is admin restricted.
 * Never print a password reset or verification link to logs.
 */
@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);
    private static final URI RESEND_URL = URI.create("https://api.resend.com/emails");

    private final ObjectProvider<JavaMailSender> sender;
    private final Deque<MailPreview> previews = new ConcurrentLinkedDeque<>();
    private final boolean preview;
    private final String from;
    private final String resendKey;
    private final HttpClient client;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.preview:false}") boolean preview,
            @Value("${app.mail.from:noreply@cholobodlai.example}") String from,
            @Value("${RESEND_API_KEY:}") String resendKey,
            @Value("${RENDER:}") String render) {
        if (preview && render != null && !render.isBlank()) {
            throw new IllegalStateException("APP_MAIL_PREVIEW cannot be enabled on Render");
        }
        this.sender = sender;
        this.preview = preview;
        this.from = from;
        this.resendKey = resendKey == null ? "" : resendKey.strip();
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public boolean deliveryReady() {
        return preview || !resendKey.isEmpty() || sender.getIfAvailable() != null;
    }

    public void send(String address, String subject, String content, String actionUrl) {
        if (!resendKey.isEmpty()) {
            sendResend(address, subject, content + "\n\n" + actionUrl);
            return;
        }
        JavaMailSender mail = sender.getIfAvailable();
        if (mail != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(address);
                message.setSubject(subject);
                message.setText(content + "\n\n" + actionUrl);
                mail.send(message);
                return;
            } catch (RuntimeException ex) {
                log.warn("SMTP delivery failed; sensitive link contents are suppressed");
                throw new IllegalStateException("Email delivery failed; please retry after the mail service is available");
            }
        }
        if (preview) {
            previews.addFirst(MailPreview.builder().address(address).subject(subject)
                    .message(content).actionUrl(actionUrl).createdAt(LocalDateTime.now()).build());
            while (previews.size() > 50) previews.pollLast();
            return;
        }
        // Missing delivery channel should be visible, not silently leave users unverified.
        throw new IllegalStateException("Email delivery is not configured. Set RESEND_API_KEY and APP_MAIL_FROM");
    }

    private void sendResend(String to, String subject, String text) {
        String body = "{\"from\":" + json(from)
                + ",\"to\":[" + json(to) + "]"
                + ",\"subject\":" + json(subject)
                + ",\"text\":" + json(text) + "}";
        HttpRequest request = HttpRequest.newBuilder(RESEND_URL)
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + resendKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "CholoBodlai/6.0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Resend rejected email delivery (HTTP {}). Sender domain or API key may be invalid", response.statusCode());
                throw new IllegalStateException("Email delivery failed. Check the verified sender domain and Resend API key");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Email service could not be reached. Please try again", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Email delivery was interrupted", ex);
        }
    }

    // JSON quoting without exposing a token in logs or requiring additional dependencies.
    static String json(String text) {
        if (text == null) return "\"\"";
        StringBuilder encoded = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> encoded.append("\\\"");
                case '\\' -> encoded.append("\\\\");
                case '\n' -> encoded.append("\\n");
                case '\r' -> encoded.append("\\r");
                case '\t' -> encoded.append("\\t");
                default -> {
                    if (ch < 32) encoded.append(String.format("\\u%04x", (int) ch));
                    else encoded.append(ch);
                }
            }
        }
        return encoded.append('"').toString();
    }

    public boolean previewEnabled() { return preview; }
    public List<MailPreview> latestPreviews() { return List.copyOf(previews); }
}
