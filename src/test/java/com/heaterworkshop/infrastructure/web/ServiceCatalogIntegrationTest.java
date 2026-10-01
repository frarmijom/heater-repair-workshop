package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.infrastructure.persistence.*;

import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:services;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cors.allowed-origins=http://localhost",
        "server.servlet.session.cookie.secure=false"
})
class ServiceCatalogIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    SpringDataUserRepository users;

    @Autowired
    SpringDataServiceCatalogRepository services;

    @Autowired
    SpringDataServiceCatalogComponentRepository serviceComponents;

    @Autowired
    PasswordEncoder encoder;

    HttpClient client;
    CookieManager cookies;

    static final String PASSWORD = "test-only-password-93";

    @BeforeEach
    void setup() {
        serviceComponents.deleteAll();
        services.deleteAll();
        users.deleteAll();

        users.save(new JpaUserEntity(
                "test-user",
                "tech@example.test",
                encoder.encode(PASSWORD),
                true,
                Instant.now()));

        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .build();
    }

    HttpResponse<String> call(
            String method,
            String path,
            String body,
            String token) throws Exception {

        var builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path))
                .method(
                        method,
                        body == null
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofString(body));

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        if (token != null) {
            builder.header("X-CSRF-TOKEN", token);
        }

        return client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    String field(String json, String name) {
        var matcher = Pattern.compile(
                "\"" + name + "\":\"([^\"]+)\"")
                .matcher(json);

        assertTrue(matcher.find(), "Missing " + name + " in " + json);
        return matcher.group(1);
    }

    String csrf() throws Exception {
        var response = call("GET", "/api/auth/csrf", null, null);

        assertEquals(200, response.statusCode());

        return field(response.body(), "token");
    }

    String authenticate() throws Exception {
        String token = csrf();

        var login = call(
                "POST",
                "/api/auth/login",
                "{\"email\":\"tech@example.test\",\"password\":\""
                        + PASSWORD + "\"}",
                token);

        assertEquals(200, login.statusCode(), login.body());

        return csrf();
    }

    long version(String body) {
        var matcher = Pattern.compile("\"version\":(\\d+)")
                .matcher(body);

        assertTrue(matcher.find(), "Missing version in " + body);
        return Long.parseLong(matcher.group(1));
    }

    @Test
    void serviceCatalogCrudWorksThroughHttp() throws Exception {
        String token = authenticate();

        var created = call(
                "POST",
                "/api/services",
                """
                {
                  "code":"MANT-001",
                  "name":"Mantención preventiva",
                  "description":"Mantención general del calefont",
                  "price":35000
                }
                """,
                token);

        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"code\":\"MANT-001\""));
        assertTrue(created.body().contains("\"name\":\"Mantención preventiva\""));
        assertTrue(created.body().contains("\"price\":35000"));
        assertTrue(created.body().contains("\"active\":true"));
        assertEquals(0, version(created.body()));

        String id = field(created.body(), "id");

        var fetched = call(
                "GET",
                "/api/services/" + id,
                null,
                null);

        assertEquals(200, fetched.statusCode(), fetched.body());
        assertEquals(id, field(fetched.body(), "id"));

        var listed = call(
                "GET",
                "/api/services",
                null,
                null);

        assertEquals(200, listed.statusCode(), listed.body());
        assertTrue(listed.body().contains("\"code\":\"MANT-001\""));

        var edited = call(
                "PATCH",
                "/api/services/" + id,
                """
                {
                  "expectedVersion":0,
                  "price":39000,
                  "name":"Mantención preventiva completa"
                }
                """,
                token);

        assertEquals(200, edited.statusCode(), edited.body());
        assertTrue(edited.body().contains("\"price\":39000"));
        assertTrue(edited.body().contains(
                "\"name\":\"Mantención preventiva completa\""));
        assertEquals(1, version(edited.body()));

        assertEquals(1, services.count());
    }

    @Test
    void serviceCompositionWorksThroughHttpAndValidatesInputs() throws Exception {
        String token = authenticate();

        // Catálogos necesarios para crear artículos reales.
        String categoryId = field(
                call(
                        "POST",
                        "/api/inventory/categories",
                        "{\"name\":\"Componentes servicio\"}",
                        token).body(),
                "id");

        String unitId = field(
                call(
                        "POST",
                        "/api/inventory/units",
                        "{\"name\":\"Unidad\",\"symbol\":\"un\",\"allowsDecimal\":false}",
                        token).body(),
                "id");

        // Artículo STANDARD.
        var standardCreated = call(
                "POST",
                "/api/inventory/items",
                "{\"sku\":\"SERV-COMP-001\","
                        + "\"name\":\"Membrana servicio\","
                        + "\"categoryId\":\"" + categoryId + "\","
                        + "\"unitId\":\"" + unitId + "\","
                        + "\"initialStock\":0,"
                        + "\"requestId\":\"service-component-standard\"}",
                token);

        assertEquals(201, standardCreated.statusCode(), standardCreated.body());
        String standardId = field(standardCreated.body(), "id");

        // Artículo KIT.
        var kitCreated = call(
                "POST",
                "/api/inventory/items",
                "{\"sku\":\"SERV-KIT-001\","
                        + "\"name\":\"Kit mantención servicio\","
                        + "\"categoryId\":\"" + categoryId + "\","
                        + "\"unitId\":\"" + unitId + "\","
                        + "\"itemType\":\"KIT\","
                        + "\"initialStock\":0,"
                        + "\"requestId\":\"service-component-kit\"}",
                token);

        assertEquals(201, kitCreated.statusCode(), kitCreated.body());
        String kitId = field(kitCreated.body(), "id");

        // Servicio.
        var serviceCreated = call(
                "POST",
                "/api/services",
                """
                {
                  "code":"SERV-COMP",
                  "name":"Servicio con repuestos",
                  "description":"Servicio de prueba",
                  "price":45000
                }
                """,
                token);

        assertEquals(201, serviceCreated.statusCode(), serviceCreated.body());
        String serviceId = field(serviceCreated.body(), "id");

        // Reemplazo completo de composición con STANDARD + KIT.
        String composition =
                "{\"components\":["
                        + "{\"inventoryItemId\":\"" + standardId + "\",\"quantity\":2},"
                        + "{\"inventoryItemId\":\"" + kitId + "\",\"quantity\":1}"
                        + "]}";

        var replaced = call(
                "PUT",
                "/api/services/" + serviceId + "/composition",
                composition,
                token);

        assertEquals(200, replaced.statusCode(), replaced.body());
        assertTrue(
                replaced.body().contains(
                        "\"inventoryItemId\":\"" + standardId + "\""),
                replaced.body());
        assertTrue(
                replaced.body().contains(
                        "\"inventoryItemId\":\"" + kitId + "\""),
                replaced.body());

        // Persistencia / lectura HTTP.
        var fetched = call(
                "GET",
                "/api/services/" + serviceId + "/composition",
                null,
                null);

        assertEquals(200, fetched.statusCode(), fetched.body());
        assertTrue(
                fetched.body().contains(
                        "\"inventoryItemId\":\"" + standardId + "\""),
                fetched.body());
        assertTrue(
                fetched.body().contains(
                        "\"inventoryItemId\":\"" + kitId + "\""),
                fetched.body());

        assertEquals(2, serviceComponents.count());

        // Campo no autorizado -> 400.
        assertEquals(
                400,
                call(
                        "PUT",
                        "/api/services/" + serviceId + "/composition",
                        "{\"components\":[],\"unexpected\":true}",
                        token).statusCode());

        // Cantidad cero -> 400.
        assertEquals(
                400,
                call(
                        "PUT",
                        "/api/services/" + serviceId + "/composition",
                        "{\"components\":[{\"inventoryItemId\":\""
                                + standardId
                                + "\",\"quantity\":0}]}",
                        token).statusCode());

        // Más de tres decimales -> 400.
        assertEquals(
                400,
                call(
                        "PUT",
                        "/api/services/" + serviceId + "/composition",
                        "{\"components\":[{\"inventoryItemId\":\""
                                + standardId
                                + "\",\"quantity\":1.0001}]}",
                        token).statusCode());

        // Artículo inexistente -> 404.
        assertEquals(
                404,
                call(
                        "PUT",
                        "/api/services/" + serviceId + "/composition",
                        "{\"components\":[{\"inventoryItemId\":\""
                                + UUID.randomUUID()
                                + "\",\"quantity\":1}]}",
                        token).statusCode());

        // Servicio inexistente -> 404.
        assertEquals(
                404,
                call(
                        "GET",
                        "/api/services/" + UUID.randomUUID() + "/composition",
                        null,
                        null).statusCode());

        // Un mismo artículo no puede repetirse -> 400.
        assertEquals(
                400,
                call(
                        "PUT",
                        "/api/services/" + serviceId + "/composition",
                        "{\"components\":["
                                + "{\"inventoryItemId\":\"" + standardId + "\",\"quantity\":1},"
                                + "{\"inventoryItemId\":\"" + standardId + "\",\"quantity\":2}"
                                + "]}",
                        token).statusCode());

        // Los intentos rechazados no deben reemplazar la composición válida.
        assertEquals(2, serviceComponents.count());
    }

}
