package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.infrastructure.persistence.*;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.cors.allowed-origins=http://localhost",
        "server.servlet.session.cookie.secure=false"})
@ExtendWith(OutputCaptureExtension.class)
class AuthenticationIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired SpringDataUserRepository users;
    @Autowired SpringDataWorkOrderRepository orders;
    @Autowired PasswordEncoder encoder;
    HttpClient client;
    CookieManager cookies;
    // Synthetic test fixture only, never an initial production credential.
    static final String PASSWORD = "test-only-password-93";

    @BeforeEach void setup() {
        orders.deleteAll();
        users.deleteAll();
        users.save(new JpaUserEntity("test-user", "tech@example.test", encoder.encode(PASSWORD), true, Instant.now()));
        users.save(new JpaUserEntity("disabled-user", "disabled@example.test", encoder.encode(PASSWORD), false, Instant.now()));
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    HttpResponse<String> call(String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        if (token != null) builder.header("X-CSRF-TOKEN", token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    String field(String json, String name) {
        var matcher = Pattern.compile("\"" + name + "\":\"([^\"]+)\"").matcher(json);
        assertTrue(matcher.find(), "Missing " + name);
        return matcher.group(1);
    }
    String csrf() throws Exception {
        var response = call("GET", "/api/auth/csrf", null, null);
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        return field(response.body(), "token");
    }
    HttpResponse<String> login(String email, String password, String token) throws Exception {
        return call("POST", "/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", token);
    }
    String sessionId() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("WORKSHOP_SESSION"))
                .findFirst().orElseThrow().getValue();
    }

    @Test void healthIsPublicAndEmptyAndAllRepairOperationsRequireAuthentication() throws Exception {
        var health = call("GET", "/api/health", null, null);
        assertEquals(200, health.statusCode());
        assertEquals("", health.body());
        assertTrue(health.headers().allValues("Set-Cookie").isEmpty());
        for (String[] endpoint : List.of(new String[]{"GET", "/api/work-orders"},
                new String[]{"GET", "/api/work-orders/ORDER-550E8400-E29B-41D4-A716-446655440001"},
                new String[]{"POST", "/api/work-orders"},
                new String[]{"PATCH", "/api/work-orders/ORDER-550E8400-E29B-41D4-A716-446655440001/start"},
                new String[]{"PATCH", "/api/work-orders/ORDER-550E8400-E29B-41D4-A716-446655440001/complete"})) {
            assertEquals(401, call(endpoint[0], endpoint[1], null, null).statusCode());
            assertEquals(401, call(endpoint[0], endpoint[1], null, csrf()).statusCode());
        }
        for (String action : List.of("diagnosis/begin", "diagnosis", "diagnosis/complete", "approve", "reject", "waiting-parts")) {
            assertEquals(401, call("PATCH", "/api/work-orders/ORDER-550E8400-E29B-41D4-A716-446655440001/" + action, "{}", null).statusCode());
        }
        assertEquals(401, call("GET", "/api/auth/session", null, null).statusCode());
    }

    @Test void rejectsUnknownWrongAndDisabledCredentialsWithoutEnumeration(CapturedOutput output) throws Exception {
        String token = csrf();
        var wrong = login("tech@example.test", "incorrect-test-only", token);
        var unknown = login("unknown@example.test", PASSWORD, token);
        var disabled = login("disabled@example.test", PASSWORD, token);
        assertEquals(401, wrong.statusCode());
        assertEquals(401, unknown.statusCode());
        assertEquals(401, disabled.statusCode());
        assertEquals(wrong.body(), unknown.body());
        assertEquals(wrong.body(), disabled.body());
        assertEquals("{\"message\":\"Invalid credentials.\"}", wrong.body());
        assertEquals(401, call("GET", "/api/auth/session", null, null).statusCode());
        String oversized = "sensitive-long-password-".repeat(10);
        assertEquals(401, login("tech@example.test", oversized, token).statusCode());
        assertFalse(output.getAll().contains(oversized));
        assertFalse(output.getAll().contains(PASSWORD));
        assertFalse(output.getAll().contains("incorrect-test-only"));
    }

    @Test void authenticatesRotatesSessionAndCsrfPreservesWorkflowAndInvalidatesLogout(CapturedOutput output) throws Exception {
        String oldToken = csrf();
        String oldSession = sessionId();
        var loggedIn = login(" TECH@example.test ", PASSWORD, oldToken);
        assertEquals(200, loggedIn.statusCode());
        assertEquals("{\"email\":\"tech@example.test\"}", loggedIn.body());
        assertNotEquals(oldSession, sessionId());
        String cookie = loggedIn.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/"));
        var savedUser = users.findByEmail("tech@example.test").orElseThrow();
        assertNotEquals(PASSWORD, savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("{bcrypt}"));
        assertTrue(encoder.matches(PASSWORD, savedUser.getPasswordHash()));
        assertFalse(loggedIn.body().contains(savedUser.getPasswordHash()));
        assertFalse(output.getAll().contains(PASSWORD));
        assertEquals(200, call("GET", "/api/auth/session", null, null).statusCode());
        assertEquals(403, call("POST", "/api/work-orders", "{}", null).statusCode());
        assertEquals(403, call("POST", "/api/work-orders", "{}", oldToken).statusCode());
        String token = csrf();
        var created = call("POST", "/api/work-orders", """
                {"customerName":"Test Customer","customerContact":"+56911112222","heaterBrand":"Bosch",
                 "heaterModel":"Therm 5700","serviceType":"REPAIR","reportedIssue":"Turns off"}
                """, token);
        assertEquals(201, created.statusCode());
        String id = field(created.body(), "id");
        assertEquals(200, call("GET", "/api/work-orders", null, null).statusCode());
        assertEquals(200, call("GET", "/api/work-orders/" + id, null, null).statusCode());
        for (String action : List.of("diagnosis/begin", "diagnosis", "diagnosis/complete", "approve", "reject", "waiting-parts", "start", "complete")) {
            assertEquals(403, call("PATCH", "/api/work-orders/" + id + "/" + action, "{}", null).statusCode());
        }
        assertEquals(409, call("PATCH", "/api/work-orders/" + id + "/start", null, token).statusCode());
        assertEquals(200, call("PATCH", "/api/work-orders/" + id + "/diagnosis/begin", null, token).statusCode());
        assertEquals(200, call("PATCH", "/api/work-orders/" + id + "/diagnosis", "{\"diagnosis\":\"Damaged sensor\"}", token).statusCode());
        assertEquals(200, call("PATCH", "/api/work-orders/" + id + "/diagnosis/complete", null, token).statusCode());
        var approved = call("PATCH", "/api/work-orders/" + id + "/approve", "{\"partsAvailable\":false}", token);
        assertEquals(200, approved.statusCode());
        assertEquals("APPROVED", field(approved.body(), "customerDecision"));
        assertEquals("WAITING_PARTS", field(approved.body(), "status"));
        var started = call("PATCH", "/api/work-orders/" + id + "/start", null, token);
        assertEquals(200, started.statusCode());
        assertEquals("IN_PROGRESS", field(started.body(), "status"));
        var completed = call("PATCH", "/api/work-orders/" + id + "/complete", null, token);
        assertEquals(200, completed.statusCode());
        assertEquals("COMPLETED", field(completed.body(), "status"));
        assertEquals(403, call("POST", "/api/auth/logout", null, null).statusCode());
        String authenticatedSession = sessionId();
        var logout = call("POST", "/api/auth/logout", null, token);
        assertEquals(204, logout.statusCode());
        assertTrue(logout.headers().allValues("Set-Cookie").stream().anyMatch(c -> c.contains("Max-Age=0")));
        assertEquals(401, call("GET", "/api/auth/session", null, null).statusCode());
        // Replay the old credential directly, independent of cookie deletion in the browser.
        client = HttpClient.newHttpClient();
        var replay = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/work-orders"))
                .header("Cookie", "WORKSHOP_SESSION=" + authenticatedSession).build();
        assertEquals(401, client.send(replay, HttpResponse.BodyHandlers.ofString()).statusCode());
        var fixation = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/session"))
                .header("Cookie", "WORKSHOP_SESSION=" + oldSession).build();
        assertEquals(401, client.send(fixation, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void malformedStoredHashDoesNotExposeEncoderDetails() throws Exception {
        users.save(new JpaUserEntity("broken-user", "broken@example.test", "{unsupported}private-hash", true, Instant.now()));
        var response = login("broken@example.test", PASSWORD, csrf());
        assertEquals(500, response.statusCode());
        assertEquals("{\"message\":\"Unable to complete the request.\"}", response.body());
        assertEquals(401, call("GET", "/api/auth/session", null, null).statusCode());
    }

    @Test void loginRequiresCsrfAndMalformedCredentialsAreSafe() throws Exception {
        assertEquals(401, login("tech@example.test", PASSWORD, null).statusCode());
        assertEquals(401, login("tech@example.test", PASSWORD, "invalid").statusCode());
        assertEquals(401, call("POST", "/api/auth/login", "{}", csrf()).statusCode());
        assertEquals(400, call("POST", "/api/auth/login", "not-json", csrf()).statusCode());
    }
}
