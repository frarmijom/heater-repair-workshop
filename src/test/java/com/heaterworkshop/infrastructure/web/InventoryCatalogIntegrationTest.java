package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.infrastructure.persistence.*;
import com.heaterworkshop.domain.inventory.InventoryMovementType;
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
        "spring.datasource.url=jdbc:h2:mem:catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.cors.allowed-origins=http://localhost",
        "server.servlet.session.cookie.secure=false"})
@ExtendWith(OutputCaptureExtension.class)
class InventoryCatalogIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired SpringDataUserRepository users;
    @Autowired SpringDataWorkOrderRepository orders;
    @Autowired PasswordEncoder encoder;
    @Autowired SpringDataInventoryCategoryRepository categories;
    @Autowired SpringDataUnitOfMeasureRepository units;
    @Autowired SpringDataInventoryItemRepository items;
    @Autowired SpringDataInventoryMovementRepository movements;
    @Autowired SpringDataInventoryItemCreationRequestRepository creationRequests;
    HttpClient client;
    CookieManager cookies;
    // Synthetic test fixture only, never an initial production credential.
    static final String PASSWORD = "test-only-password-93";

    @BeforeEach void setup() {
        creationRequests.deleteAll(); movements.deleteAll(); items.deleteAll();
        categories.deleteAll(); units.deleteAll();
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

    String authenticate() throws Exception {
        assertEquals(200, login("tech@example.test", PASSWORD, csrf()).statusCode()); return csrf();
    }
    long version(String body) { var m=Pattern.compile("\"version\":(\\d+)").matcher(body); assertTrue(m.find()); return Long.parseLong(m.group(1)); }

    @Test void categoriesPreserveIdentityAndRejectStaleEditsAndDuplicates() throws Exception {
        String token=authenticate();
        var created=call("POST","/api/inventory/categories","{\"name\":\"  Calefont   Gas  \"}",token);
        assertEquals(201,created.statusCode()); assertEquals("Calefont Gas",field(created.body(),"name"));
        String id=field(created.body(),"id"), date=field(created.body(),"createdAt");
        assertEquals(409,call("POST","/api/inventory/categories","{\"name\":\"CALEFONT GAS\"}",token).statusCode());
        var edited=call("PATCH","/api/inventory/categories/"+id,"{\"name\":\"Repuestos\",\"active\":false,\"expectedVersion\":0}",token);
        assertEquals(200,edited.statusCode()); assertEquals(1,version(edited.body()));
        assertEquals(id,field(edited.body(),"id")); assertEquals(date,field(edited.body(),"createdAt"));
        assertTrue(edited.body().contains("\"active\":false"));
        assertEquals(409,call("POST","/api/inventory/categories","{\"name\":\"repuestos\"}",token).statusCode());
        assertEquals(409,call("PATCH","/api/inventory/categories/"+id,"{\"active\":true,\"expectedVersion\":0}",token).statusCode());
        assertEquals(200,call("PATCH","/api/inventory/categories/"+id,"{\"active\":true,\"expectedVersion\":1}",token).statusCode());
        assertTrue(call("GET","/api/inventory/categories",null,null).body().contains("Repuestos"));
        assertEquals(1,categories.count());
    }
    @Test void unitsAreAdministrableAndSymbolsAndNamesAreUnique() throws Exception {
        String token=authenticate();
        var created=call("POST","/api/inventory/units","{\"name\":\"Metro\",\"symbol\":\"m\",\"allowsDecimal\":true}",token);
        assertEquals(201,created.statusCode()); String id=field(created.body(),"id");
        assertEquals(409,call("POST","/api/inventory/units","{\"name\":\"Metros\",\"symbol\":\"M\",\"allowsDecimal\":false}",token).statusCode());
        assertEquals(409,call("POST","/api/inventory/units","{\"name\":\"METRO\",\"symbol\":\"mt\",\"allowsDecimal\":false}",token).statusCode());
        var edited=call("PATCH","/api/inventory/units/"+id,"{\"expectedVersion\":0,\"name\":\"Unidad\",\"symbol\":\"un\",\"allowsDecimal\":false,\"active\":false}",token);
        assertEquals(200,edited.statusCode()); assertTrue(edited.body().contains("\"allowsDecimal\":false"));
        assertEquals(409,call("PATCH","/api/inventory/units/"+id,"{\"expectedVersion\":0,\"active\":true}",token).statusCode());
        assertEquals(200,call("PATCH","/api/inventory/units/"+id,"{\"expectedVersion\":1,\"active\":true}",token).statusCode());
        assertTrue(call("GET","/api/inventory/units",null,null).body().contains("Unidad"));
        assertEquals(1,units.count());
    }
    @Test void rejectsInvalidOrProtectedFieldsAndMissingResources() throws Exception {
        String token=authenticate();
        assertEquals(400,call("PATCH","/api/inventory/categories/invalid","{\"expectedVersion\":0,\"active\":false}",token).statusCode());
        for(String body:List.of("{}","{\"name\":null}","{\"name\":\"  \"}","{\"name\":42}","{\"name\":\"Good\",\"active\":false}","{\"name\":\"Good\",\"id\":\"forged\"}","{\"name\":\""+"x".repeat(121)+"\"}"))
            assertEquals(400,call("POST","/api/inventory/categories",body,token).statusCode(),body);
        for(String body:List.of("{\"name\":\"Unit\",\"symbol\":\"\",\"allowsDecimal\":true}","{\"name\":\"Unit\",\"symbol\":\"un\"}","{\"name\":\"Unit\",\"symbol\":\"un\",\"allowsDecimal\":\"true\"}"))
            assertEquals(400,call("POST","/api/inventory/units",body,token).statusCode());
        for(String body:List.of("{}","{\"expectedVersion\":0}","{\"expectedVersion\":-1,\"active\":true}","{\"expectedVersion\":0.5,\"active\":true}","{\"expectedVersion\":0,\"name\":null}","{\"expectedVersion\":0,\"active\":null}","{\"expectedVersion\":0,\"stock\":1}"))
            assertEquals(400,call("PATCH","/api/inventory/categories/"+UUID.randomUUID(),body,token).statusCode(),body);
        assertEquals(404,call("PATCH","/api/inventory/units/"+UUID.randomUUID(),"{\"expectedVersion\":0,\"active\":false}",token).statusCode());
    }
    @Test void concurrentEditsHaveExactlyOneWinner() throws Exception {
        String token=authenticate();
        for(String resource:List.of("categories","units")) {
            String payload=resource.equals("units") ? "{\"name\":\"Unit\",\"symbol\":\"un\",\"allowsDecimal\":false}" : "{\"name\":\"Category\"}";
            String id=field(call("POST","/api/inventory/"+resource,payload,token).body(),"id");
            var barrier=new java.util.concurrent.CyclicBarrier(2);
            java.util.function.Supplier<Integer> edit=()-> {
                try { barrier.await(); return call("PATCH","/api/inventory/"+resource+"/"+id,"{\"expectedVersion\":0,\"active\":false}",token).statusCode(); }
                catch(Exception ex) { throw new RuntimeException(ex); }
            };
            var first=java.util.concurrent.CompletableFuture.supplyAsync(edit);
            var second=java.util.concurrent.CompletableFuture.supplyAsync(edit);
            assertEquals(List.of(200,409),java.util.stream.Stream.of(first.join(),second.join()).sorted().toList());
        }
    }

    @Test void inventoryRequiresSessionAndCsrfAndOffersNoDelete() throws Exception {
        for(String resource:List.of("categories","units")) {
            String path="/api/inventory/"+resource;
            assertEquals(401,call("GET",path,null,null).statusCode());
            assertEquals(401,call("POST",path,"{}",null).statusCode());
            assertEquals(401,call("PATCH",path+"/"+UUID.randomUUID(),"{}",null).statusCode());
        }
        String token=authenticate();
        for(String resource:List.of("categories","units")) {
            String path="/api/inventory/"+resource;
            assertEquals(200,call("GET",path,null,null).statusCode());
            assertEquals(403,call("POST",path,"{}",null).statusCode());
            assertEquals(403,call("PATCH",path+"/"+UUID.randomUUID(),"{}","invalid").statusCode());
            assertNotEquals(200,call("DELETE",path+"/"+UUID.randomUUID(),null,token).statusCode());
        }
    }

    @Test void itemCreationInitialStockIdempotencyAndSnapshotsAreAtomic() throws Exception {
        String token=authenticate();
        String categoryId=field(call("POST","/api/inventory/categories","{\"name\":\"Repuestos\"}",token).body(),"id");
        String unitId=field(call("POST","/api/inventory/units","{\"name\":\"Metro\",\"symbol\":\"m\",\"allowsDecimal\":true}",token).body(),"id");
        String body="{\"sku\":\"VALV-001\",\"name\":\"Válvula\",\"description\":\"\",\"categoryId\":\""+categoryId+"\",\"unitId\":\""+unitId+"\",\"stockMinimum\":10,\"referenceUnitCost\":2.1250,\"initialStock\":9,\"requestId\":\"create-valv-1\"}";
        var created=call("POST","/api/inventory/items",body,token);
        assertEquals(201,created.statusCode(),created.body());
        String id=field(created.body(),"id");
        assertTrue(created.body().contains("\"stockCurrent\":9.000"));
        assertTrue(created.body().contains("\"lowStock\":true"));
        assertTrue(created.body().contains("\"hasMovements\":true"));
        var movement=movements.findByRequestId("create-valv-1").orElseThrow().toDomain();
        assertEquals(InventoryMovementType.INITIAL_ENTRY,movement.type());
        assertEquals(new java.math.BigDecimal("9.000"),movement.quantity());
        assertEquals(new java.math.BigDecimal("0.000"),movement.stockBefore());
        assertEquals(new java.math.BigDecimal("9.000"),movement.stockAfter());
        assertEquals(new java.math.BigDecimal("2.1250"),movement.unitCostSnapshot());
        assertEquals("VALV-001",movement.skuSnapshot());
        assertEquals("Válvula",movement.itemNameSnapshot());
        assertEquals("Metro",movement.unitNameSnapshot());
        assertEquals("m",movement.unitSymbolSnapshot());
        assertEquals("tech@example.test",movement.actor());
        assertEquals(id,field(call("POST","/api/inventory/items",body,token).body(),"id"));
        assertEquals(1,items.count());
        assertEquals(1,movements.count());
        assertEquals(409,call("POST","/api/inventory/items",body.replace("\"initialStock\":9","\"initialStock\":8"),token).statusCode());
        String duplicateSku=body.replace("VALV-001","valv-001").replace("create-valv-1","create-valv-duplicate");
        assertEquals(409,call("POST","/api/inventory/items",duplicateSku,token).statusCode());
        assertEquals(1,items.count());
        assertEquals(id,field(call("GET","/api/inventory/items/"+id,null,null).body(),"id"));
        assertTrue(call("GET","/api/inventory/items",null,null).body().contains("\"category\":{\"id\":"));
    }

    @Test void zeroStockCreationHasNoMovementAndCanCorrectUnitBeforeHistory() throws Exception {
        String token=authenticate();
        String categoryId=field(call("POST","/api/inventory/categories","{\"name\":\"Consumibles\"}",token).body(),"id");
        String unitOne=field(call("POST","/api/inventory/units","{\"name\":\"Unidad\",\"symbol\":\"un\",\"allowsDecimal\":false}",token).body(),"id");
        String unitTwo=field(call("POST","/api/inventory/units","{\"name\":\"Metro\",\"symbol\":\"m\",\"allowsDecimal\":true}",token).body(),"id");
        String payload="{\"sku\":\"CABLE-001\",\"name\":\"Cable\",\"categoryId\":\""+categoryId+"\",\"unitId\":\""+unitOne+"\",\"initialStock\":0,\"requestId\":\"create-cable-1\"}";
        var created=call("POST","/api/inventory/items",payload,token);
        assertEquals(201,created.statusCode(),created.body());
        String itemId=field(created.body(),"id");
        assertEquals(0,movements.count());
        assertEquals(200,call("PATCH","/api/inventory/items/"+itemId,"{\"expectedVersion\":0,\"unitId\":\""+unitTwo+"\"}",token).statusCode());
        assertEquals(0,movements.count());
        var decimalItem=call("POST","/api/inventory/items",payload.replace("CABLE-001","CABLE-002").replace("create-cable-1","create-cable-2").replace(unitTwo,unitOne).replace("\"initialStock\":0","\"initialStock\":2.5"),token);
        assertEquals(400,decimalItem.statusCode());
        assertEquals(1,items.count());
        assertEquals(0,movements.count());
    }

    @Test void acceptsExactDecimalStringsWithoutJavaScriptNumberRounding() throws Exception {
        String token=authenticate();
        String categoryId=field(call("POST","/api/inventory/categories","{\"name\":\"Cableado\"}",token).body(),"id");
        String unitId=field(call("POST","/api/inventory/units","{\"name\":\"Metro\",\"symbol\":\"m\",\"allowsDecimal\":true}",token).body(),"id");
        String body="{\"sku\":\"CABLE-DECIMAL\",\"name\":\"Cable decimal\",\"categoryId\":\""+categoryId+"\",\"unitId\":\""+unitId+"\",\"stockMinimum\":\"1.250\",\"referenceUnitCost\":\"2.1250\",\"initialStock\":\"1.500\",\"requestId\":\"decimal-string-1\"}";
        var created=call("POST","/api/inventory/items",body,token);
        assertEquals(201,created.statusCode(),created.body());
        assertTrue(created.body().contains("\"stockCurrent\":1.500"));
        assertEquals(new java.math.BigDecimal("1.500"),movements.findByRequestId("decimal-string-1").orElseThrow().toDomain().quantity());
    }

    @Test void editsAreVersionedUnitLocksAndDeactivationPreservesStock() throws Exception {
        String token=authenticate();
        String categoryId=field(call("POST","/api/inventory/categories","{\"name\":\"Filtros\"}",token).body(),"id");
        String unitOne=field(call("POST","/api/inventory/units","{\"name\":\"Unidad\",\"symbol\":\"un\",\"allowsDecimal\":false}",token).body(),"id");
        String unitTwo=field(call("POST","/api/inventory/units","{\"name\":\"Caja\",\"symbol\":\"cj\",\"allowsDecimal\":false}",token).body(),"id");
        String payload="{\"sku\":\"FILTER-001\",\"name\":\"Filtro\",\"categoryId\":\""+categoryId+"\",\"unitId\":\""+unitOne+"\",\"initialStock\":2,\"stockMinimum\":1,\"requestId\":\"create-filter-1\"}";
        var created=call("POST","/api/inventory/items",payload,token);
        String id=field(created.body(),"id");
        assertEquals(409,call("PATCH","/api/inventory/items/"+id,"{\"expectedVersion\":1,\"unitId\":\""+unitTwo+"\"}",token).statusCode());
        var edited=call("PATCH","/api/inventory/items/"+id,"{\"expectedVersion\":1,\"name\":\"Filtro reforzado\"}",token);
        assertEquals(200,edited.statusCode(),edited.body());
        assertEquals(409,call("PATCH","/api/inventory/items/"+id,"{\"expectedVersion\":1,\"active\":false}",token).statusCode());
        var inactive=call("PATCH","/api/inventory/items/"+id,"{\"expectedVersion\":2,\"active\":false}",token);
        assertEquals(200,inactive.statusCode());
        assertTrue(inactive.body().contains("\"stockCurrent\":2.000"));
        assertTrue(inactive.body().contains("\"active\":false"));
        assertEquals(200,call("PATCH","/api/inventory/items/"+id,"{\"expectedVersion\":3,\"active\":true}",token).statusCode());
    }

    @Test void rejectsInvalidFieldsInactiveCatalogsAndUnauthenticatedWrites() throws Exception {
        assertEquals(401,call("GET","/api/inventory/items",null,null).statusCode());
        assertEquals(401,call("POST","/api/inventory/items","{}",null).statusCode());
        String token=authenticate();
        assertEquals(403,call("POST","/api/inventory/items","{}",null).statusCode());
        String categoryId=field(call("POST","/api/inventory/categories","{\"name\":\"Inactiva\"}",token).body(),"id");
        String unitId=field(call("POST","/api/inventory/units","{\"name\":\"Un\",\"symbol\":\"u\",\"allowsDecimal\":false}",token).body(),"id");
        call("PATCH","/api/inventory/categories/"+categoryId,"{\"expectedVersion\":0,\"active\":false}",token);
        String payload="{\"sku\":\"INVALID-1\",\"name\":\"Invalid\",\"categoryId\":\""+categoryId+"\",\"unitId\":\""+unitId+"\",\"requestId\":\"invalid-1\"}";
        assertEquals(409,call("POST","/api/inventory/items",payload,token).statusCode());
        assertEquals(400,call("POST","/api/inventory/items",payload.replace("\"requestId\":\"invalid-1\"","\"requestId\":\"invalid-2\",\"stockCurrent\":5"),token).statusCode());
        String activeCategory=field(call("POST","/api/inventory/categories","{\"name\":\"Activa\"}",token).body(),"id");
        call("PATCH","/api/inventory/units/"+unitId,"{\"expectedVersion\":0,\"active\":false}",token);
        assertEquals(409,call("POST","/api/inventory/items",payload.replace(categoryId,activeCategory)
            .replace("INVALID-1","INVALID-2").replace("invalid-1","invalid-3"),token).statusCode());
        assertEquals(0,items.count());
    }
}
