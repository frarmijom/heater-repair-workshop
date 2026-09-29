package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaInventoryPersistenceTest {
    @Test void roundTripsExactItemValuesAndImmutableMovementSnapshots() {
        var configuration = new Configuration().addAnnotatedClass(JpaInventoryItemEntity.class)
                .addAnnotatedClass(JpaInventoryMovementEntity.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            var factoryAdapter = new JpaRepositoryFactory(em);
            var itemJpa = factoryAdapter.getRepository(SpringDataInventoryItemRepository.class);
            var movementJpa = factoryAdapter.getRepository(SpringDataInventoryMovementRepository.class);
            var items = new JpaInventoryItemRepositoryAdapter(itemJpa);
            var movements = new JpaInventoryMovementRepositoryAdapter(movementJpa, itemJpa);
            UnitOfMeasure unit = UnitOfMeasure.create("Metro", "m", true);
            InventoryItem item = InventoryItem.create(" filter-7 ", "Filtro", "Descripción", UUID.randomUUID(),
                    unit.id(), new BigDecimal("1.250"), new BigDecimal("9.8765"));

            em.getTransaction().begin();
            InventoryItem stored = items.create(item);
            em.getTransaction().commit();
            em.clear();
            InventoryItem loaded = items.findById(item.id()).orElseThrow();
            assertEquals("FILTER-7", loaded.sku());
            assertEquals(new BigDecimal("0.000"), loaded.stockCurrent());
            assertEquals(new BigDecimal("1.250"), loaded.stockMinimum());
            assertEquals(new BigDecimal("9.8765"), loaded.referenceUnitCost());
            assertEquals(0, loaded.version());

            InventoryMovement movement = InventoryMovement.create(loaded, unit, InventoryMovementType.ENTRY,
                    InventoryMovementDirection.INCREASE, new BigDecimal("2.750"), loaded.referenceUnitCost(),
                    "req-1", "operator-1", "Restock", "RECEIPT", "r-1", null, null);
            em.getTransaction().begin();
            movements.append(movement);
            InventoryItem changed = items.saveStock(loaded.applyMovement(movement, true));
            em.getTransaction().commit();
            em.clear();

            InventoryMovement snapshot = movements.findByRequestId("req-1").orElseThrow();
            assertEquals(new BigDecimal("2.750"), snapshot.quantity());
            assertEquals(new BigDecimal("0.000"), snapshot.stockBefore());
            assertEquals(new BigDecimal("2.750"), snapshot.stockAfter());
            assertEquals("FILTER-7", snapshot.skuSnapshot());
            assertEquals("Filtro", snapshot.itemNameSnapshot());
            assertEquals("Metro", snapshot.unitNameSnapshot());
            assertEquals("m", snapshot.unitSymbolSnapshot());
            assertEquals(new BigDecimal("9.8765"), snapshot.unitCostSnapshot());
            assertEquals(new BigDecimal("2.750"), items.findById(item.id()).orElseThrow().stockCurrent());
            assertEquals(1, changed.version());
            assertEquals(1, items.findById(item.id()).orElseThrow().version());
            assertNotNull(stored.createdAt());
        }
    }
}