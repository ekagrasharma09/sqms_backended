import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SQMSServer {
    private static final int PORT = 8000;
    private static final Path BASE_DIR = Path.of("").toAbsolutePath();
    private static final String MYSQL_SERVER_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sqms_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    private static final String PASSWORD_SALT = "sqms-demo-salt";

    public static void main(String[] args) throws Exception {
        createTablesIfNeeded();

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/tokens", new TokensHandler());
        server.createContext("/api/tokens/serve-next", new ServeNextHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("SQMS Java backend running at http://localhost:" + PORT);
        System.out.println("Login with username: admin and password: 1234");
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createTablesIfNeeded() throws Exception {
        try (Connection conn = DriverManager.getConnection(MYSQL_SERVER_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS sqms_db");
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(128) NOT NULL" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS tokens (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "service VARCHAR(100) NOT NULL, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'waiting', " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "served_at TIMESTAMP NULL" +
                ")"
            );

            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO users (username, password_hash) VALUES (?, ?)"
            )) {
                ps.setString(1, "admin");
                ps.setString(2, hashPassword("1234"));
                ps.executeUpdate();
            }
        }
    }

    private static String hashPassword(String password) throws Exception {
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            PASSWORD_SALT.getBytes(StandardCharsets.UTF_8),
            100000,
            256
        );
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .getEncoded();
        return toHex(hash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> values = new HashMap<>();
        String text = json.trim();
        if (text.startsWith("{")) {
            text = text.substring(1);
        }
        if (text.endsWith("}")) {
            text = text.substring(0, text.length() - 1);
        }

        String[] pairs = text.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                values.put(cleanJsonString(parts[0]), cleanJsonString(parts[1]));
            }
        }
        return values;
    }

    private static String cleanJsonString(String value) {
        String text = value.trim();
        if (text.startsWith("\"")) {
            text = text.substring(1);
        }
        if (text.endsWith("\"")) {
            text = text.substring(0, text.length() - 1);
        }
        return URLDecoder.decode(text.replace("\\\"", "\""), StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static boolean isSamePassword(String rawPassword, String passwordHash) throws Exception {
        return MessageDigest.isEqual(
            hashPassword(rawPassword).getBytes(StandardCharsets.UTF_8),
            passwordHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                Map<String, String> data = parseJsonObject(readBody(exchange));
                String username = data.getOrDefault("username", "").trim();
                String password = data.getOrDefault("password", "");

                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ?"
                     )) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && isSamePassword(password, rs.getString("password_hash"))) {
                            sendJson(exchange, 200, "{\"ok\":true}");
                            return;
                        }
                    }
                }

                sendJson(exchange, 401, "{\"ok\":false,\"error\":\"Invalid login\"}");
            } catch (Exception error) {
                sendJson(exchange, 500, "{\"error\":\"" + jsonEscape(error.getMessage()) + "\"}");
            }
        }
    }

    static class TokensHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    listTokens(exchange);
                    return;
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    createToken(exchange);
                    return;
                }
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            } catch (Exception error) {
                sendJson(exchange, 500, "{\"error\":\"" + jsonEscape(error.getMessage()) + "\"}");
            }
        }

        private void listTokens(HttpExchange exchange) throws Exception {
            StringBuilder tokens = new StringBuilder();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, service, status, created_at, served_at " +
                     "FROM tokens WHERE status = 'waiting' ORDER BY id"
                 );
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (tokens.length() > 0) {
                        tokens.append(",");
                    }
                    tokens.append(tokenJson(rs));
                }
            }
            sendJson(exchange, 200, "{\"tokens\":[" + tokens + "]}");
        }

        private void createToken(HttpExchange exchange) throws Exception {
            Map<String, String> data = parseJsonObject(readBody(exchange));
            String name = data.getOrDefault("name", "").trim();
            String service = data.getOrDefault("service", "").trim();

            if (name.isEmpty() || service.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"Name and service are required\"}");
                return;
            }

            int tokenId;
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tokens (name, service) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS
                 )) {
                ps.setString(1, name);
                ps.setString(2, service);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    tokenId = keys.getInt(1);
                }
            }

            sendJson(exchange, 201, "{\"token\":" + getTokenJson(tokenId) + "}");
        }
    }

    static class ServeNextHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                int tokenId;

                try (PreparedStatement select = conn.prepareStatement(
                    "SELECT id FROM tokens WHERE status = 'waiting' ORDER BY id LIMIT 1 FOR UPDATE"
                );
                     ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        sendJson(exchange, 404, "{\"error\":\"No tokens to serve\"}");
                        return;
                    }
                    tokenId = rs.getInt("id");
                }

                try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE tokens SET status = 'served', served_at = ? WHERE id = ?"
                )) {
                    update.setObject(1, LocalDateTime.now());
                    update.setInt(2, tokenId);
                    update.executeUpdate();
                }

                conn.commit();
                sendJson(exchange, 200, "{\"token\":" + getTokenJson(tokenId) + "}");
            } catch (Exception error) {
                sendJson(exchange, 500, "{\"error\":\"" + jsonEscape(error.getMessage()) + "\"}");
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if ("/".equals(requestPath)) {
                requestPath = "/login.html";
            }

            Path file = BASE_DIR.resolve(requestPath.substring(1)).normalize();
            if (!file.startsWith(BASE_DIR) || !Files.exists(file) || Files.isDirectory(file)) {
                sendJson(exchange, 404, "{\"error\":\"File not found\"}");
                return;
            }

            byte[] response = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType(file));
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

        private String contentType(Path file) {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".html")) {
                return "text/html; charset=utf-8";
            }
            if (name.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (name.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            }
            return "application/octet-stream";
        }
    }

    private static String getTokenJson(int tokenId) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, name, service, status, created_at, served_at FROM tokens WHERE id = ?"
             )) {
            ps.setInt(1, tokenId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return tokenJson(rs);
                }
            }
        }
        return "{}";
    }

    private static String tokenJson(ResultSet rs) throws SQLException {
        return "{" +
            "\"id\":" + rs.getInt("id") + "," +
            "\"name\":\"" + jsonEscape(rs.getString("name")) + "\"," +
            "\"service\":\"" + jsonEscape(rs.getString("service")) + "\"," +
            "\"status\":\"" + jsonEscape(rs.getString("status")) + "\"," +
            "\"createdAt\":\"" + jsonEscape(String.valueOf(rs.getTimestamp("created_at"))) + "\"," +
            "\"servedAt\":\"" + jsonEscape(String.valueOf(rs.getTimestamp("served_at"))) + "\"" +
            "}";
    }
}
