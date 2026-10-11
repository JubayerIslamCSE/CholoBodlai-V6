package com.jubayer.cholobodlai.Service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pure HTTP fake tests: require neither MongoDB nor a Cloudinary account. */
class CloudinaryMediaGatewayTest {
    @Test
    void uploadPublicPhotoAndAuthenticatedDocument() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            String content = new String(request.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertEquals("Basic " + Base64.getEncoder().encodeToString("TESTKEY:TESTSECRET".getBytes(StandardCharsets.UTF_8)),
                    request.getRequestHeaders().getFirst("Authorization"));
            assertTrue(content.contains("name=\"public_id\""));
            int pos = content.indexOf("name=\"public_id\"\r\n\r\n") + "name=\"public_id\"\r\n\r\n".length();
            String id = content.substring(pos, content.indexOf("\r\n", pos));
            if (id.contains("truck-documents")) assertTrue(content.contains("authenticated"));
            if (id.contains("truck-photos")) assertTrue(content.contains("name=\"type\"\r\n\r\nupload"));
            byte[] result = ("{\"public_id\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(200, result.length);
            request.getResponseBody().write(result);
            request.close();
        });
        server.start();
        try {
            var client = new CloudinaryMediaGateway("fakecloud", "TESTKEY", "TESTSECRET",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
            String image = client.uploadPhoto(new byte[] {(byte)255,(byte)216,(byte)255}, "jpg");
            assertTrue(image.startsWith("cld:image:upload:"));
            assertTrue(client.photoUrl(image).startsWith("https://res.cloudinary.com/fakecloud/image/upload/"));
            String doc = client.uploadDocument("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), "pdf");
            assertTrue(doc.startsWith("cld:raw:authenticated:"));
            String privateUrl = client.privateDocumentUrl(doc);
            assertTrue(privateUrl.startsWith("https://api.cloudinary.com/v1_1/fakecloud/raw/download?"));
            assertTrue(privateUrl.contains("expires_at="));
            assertTrue(privateUrl.contains("type=authenticated"));
            assertTrue(privateUrl.contains("signature="));
            assertThrows(IllegalArgumentException.class, () -> client.photoUrl(doc));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void signatureUsesCanonicalParametersAndApiSecret() {
        var client = new CloudinaryMediaGateway("fakecloud", "TESTKEY", "SECRET");
        assertEquals("27ed24b99b10d6b19eeeb62a05e6d537022f5499",
                client.signature(Map.of("b", "2", "a", "1")));
    }

    @Test
    void malformedDocumentIdsAreRejected() {
        var client = new CloudinaryMediaGateway("fakecloud", "TESTKEY", "SECRET");
        assertThrows(IllegalArgumentException.class, () -> client.privateDocumentUrl("../some-file"));
        assertThrows(IllegalArgumentException.class, () -> client.privateDocumentUrl("cld:raw:upload:aGVsbG8:pdf"));
    }
}
