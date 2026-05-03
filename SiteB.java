import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/*
 * SiteB.java (updated)
 * Consumes XML from SiteA and serves its local copies.
 *
 * Endpoints:
 *   GET /pull          -> downloads students.xml & faculty.xml from SiteA and stores locally
 *   GET /students.xml  -> serves local copy
 *   GET /faculty.xml   -> serves local copy
 *
 * How to run:
 *   javac SiteB.java
 *   java SiteB
 * Then open: http://localhost:8082/pull
 */

public class SiteB {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static byte[] httpGetBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("GET " + url + " -> " + res.statusCode());
        }
        return res.body();
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

        server.createContext("/pull", SiteB::handlePull);
        server.createContext("/students.xml", ex -> serveFile(ex, "students.xml"));
        server.createContext("/faculty.xml",  ex -> serveFile(ex, "faculty.xml"));

        server.setExecutor(null);
        System.out.println("SiteB running on http://localhost:8082  (call /pull to sync from A)");
        server.start();
    }

    private static void handlePull(HttpExchange exchange) throws IOException {
        try {
            byte[] s = httpGetBytes("http://localhost:8081/students.xml");
            byte[] f = httpGetBytes("http://localhost:8081/faculty.xml");
            Files.write(new File("students.xml").toPath(), s);
            Files.write(new File("faculty.xml").toPath(), f);

            byte[] ok = "Pulled XML from SiteA".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(ok); }
        } catch (Exception ex) {
            byte[] msg = ("Pull failed: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, msg.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
        } finally {
            exchange.close();
        }
    }

    private static void serveFile(HttpExchange exchange, String filename) throws IOException {
        try {
            File f = new File(filename);
            if (!f.exists()) { exchange.sendResponseHeaders(404, -1); return; }
            byte[] bytes = Files.readAllBytes(f.toPath());
            exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        } finally {
            exchange.close();
        }
    }
}
