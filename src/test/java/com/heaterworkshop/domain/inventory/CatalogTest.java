package com.heaterworkshop.domain.inventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class CatalogTest {
    @Test void normalizesAndKeepsStableIdentity() {
        var category=InventoryCategory.create("  Cafe\u0301   y gas  ");
        assertEquals("Café y gas",category.name());
        assertEquals("café y gas",CatalogText.key(category.name()));
        var edited=category.edit("Nuevo",false);
        assertEquals(category.id(),edited.id()); assertEquals(category.createdAt(),edited.createdAt()); assertFalse(edited.active());
        var unit=UnitOfMeasure.create("Metro"," m ",true);
        assertEquals("m",unit.symbol()); assertTrue(unit.allowsDecimal());
        var next=unit.edit("Unidad","un",false,false);
        assertEquals(unit.id(),next.id()); assertFalse(next.allowsDecimal()); assertFalse(next.active());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"   ","\u0000","\u00a0"})
    void rejectsInvalidNames(String value) {
        assertThrows(IllegalArgumentException.class,()->InventoryCategory.create(value));
        assertThrows(IllegalArgumentException.class,()->UnitOfMeasure.create("Unidad",value,false));
    }
    @Test void rejectsLengthsAndInvalidRestoredMetadata() {
        assertThrows(IllegalArgumentException.class,()->InventoryCategory.create("x".repeat(121)));
        assertThrows(IllegalArgumentException.class,()->UnitOfMeasure.create("Unidad","x".repeat(17),false));
        var value=InventoryCategory.create("Gas");
        assertThrows(IllegalArgumentException.class,()->new InventoryCategory(value.id(),"Gas",true,-1,value.createdAt(),value.updatedAt()));
        assertThrows(IllegalArgumentException.class,()->new UnitOfMeasure(value.id(),"Unidad","un",false,true,0,value.createdAt(),value.createdAt().minusSeconds(1)));
    }
}
