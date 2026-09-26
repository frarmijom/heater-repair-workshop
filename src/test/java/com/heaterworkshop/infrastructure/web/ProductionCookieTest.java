package com.heaterworkshop.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:secure;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.cors.allowed-origins=http://localhost",
        "spring.profiles.active=prod"})
class ProductionCookieTest {
    @Value("${local.server.port}") int port;
    @Test void productionSessionCookieIsSecureEvenBehindHttpReverseProxy() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/auth/csrf")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith("WORKSHOP_SESSION="));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertFalse(cookie.contains("Domain="));
    }
}
