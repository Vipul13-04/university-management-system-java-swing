import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;

/*
 * SiteA.java
 * Publishes students.xml and faculty.xml over HTTP
 *
 * How to run:
 *   javac SiteA.java
 *   java SiteA
 * Visit:
 *   http://localhost:8081/students.xml
 *   http://localhost:8081/faculty.xml
 */

public class SiteA {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/students.xml", exchange -> serveFile(exchange, "students.xml"));
        server.createContext("/faculty.xml",  exchange -> serveFile(exchange, "faculty.xml"));

        server.setExecutor(null);
        System.out.println("SiteA running on http://localhost:8081  (serving students.xml & faculty.xml)");
        server.start();
    }

    private static void serveFile(HttpExchange exchange, String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        byte[] bytes = Files.readAllBytes(f.toPath());
        exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        exchange.close();
    }
}
