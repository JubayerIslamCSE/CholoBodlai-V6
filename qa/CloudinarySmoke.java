package com.jubayer.cholobodlai.Service;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public final class CloudinarySmoke {
    public static void main(String[] args) throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/", ex -> {
            byte[] body = ex.getRequestBody().readAllBytes();
            String text = new String(body, StandardCharsets.ISO_8859_1);
            if (ex.getRequestURI().getPath().endsWith("/upload")) {
                if (!ex.getRequestHeaders().getFirst("Authorization").equals("Basic " + Base64.getEncoder().encodeToString("KEY:SECRET".getBytes(StandardCharsets.UTF_8)))) {
                    throw new IllegalStateException("Missing Basic auth");
                }
                int start=text.indexOf("name=\"public_id\"\r\n\r\n");
                if (start<0) throw new IllegalStateException("Public ID missing");
                start += "name=\"public_id\"\r\n\r\n".length();
                String id=text.substring(start,text.indexOf("\r\n",start));
                if (id.contains("truck-documents") && !text.contains("authenticated")) throw new IllegalStateException("Document not protected");
                if (id.contains("truck-photos") && !text.contains("name=\"type\"\r\n\r\nupload")) throw new IllegalStateException("Photo not public");
                byte[] result=("{\"public_id\":\""+id+"\"}").getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200,result.length);
                ex.getResponseBody().write(result);
            } else {
                ex.sendResponseHeaders(200,-1);
            }
            ex.close();
        });
        http.start();
        try {
            CloudinaryMediaGateway g=new CloudinaryMediaGateway("testcloud","KEY","SECRET",URI.create("http://127.0.0.1:"+http.getAddress().getPort()));
            String photo=g.uploadPhoto(new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF},"jpg");
            String document=g.uploadDocument("%PDF-".getBytes(StandardCharsets.US_ASCII),"pdf");
            if (!g.photoUrl(photo).startsWith("https://res.cloudinary.com/testcloud/image/upload/cholobodlai/truck-photos/")) throw new AssertionError("Bad photo URL");
            String privateUrl = g.privateDocumentUrl(document);
            if (!privateUrl.startsWith("https://api.cloudinary.com/v1_1/testcloud/raw/download?")) throw new AssertionError("Missing raw download");
            if (!privateUrl.contains("type=authenticated") || !privateUrl.contains("expires_at=") || !privateUrl.contains("signature=")) throw new AssertionError("Missing private signature params");
            Map<String,String> sorted=new TreeMap<>();
            sorted.put("a","1");sorted.put("b","2");
            if (!g.signature(sorted).equals("27ed24b99b10d6b19eeeb62a05e6d537022f5499")) {
                throw new AssertionError("Signature mismatch: "+g.signature(sorted));
            }
            g.deleteQuietly(photo);
            System.out.println("Cloudinary public image, authenticated private document, and upload API smoke tests passed");
        } finally { http.stop(0); }
    }
}
