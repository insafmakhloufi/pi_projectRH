package Services.evenement.ticket;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;

public class TicketServer {

    private HttpServer server;
    private final int port = 8765;

    public String start(Path htmlPath) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/ticket", exchange -> {
            try {
                byte[] bytes = Files.readAllBytes(htmlPath);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            }
        });

        server.setExecutor(null);
        server.start();

        String ip = getLocalIpAddress();
        return "http://" + ip + ":" + port + "/ticket";
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (!ip.contains(":")) return ip;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}