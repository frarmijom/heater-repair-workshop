package com.heaterworkshop.domain.inventory;
public final class CatalogNotFoundException extends RuntimeException {
    public CatalogNotFoundException() { super("No se encontró el registro del catálogo."); }
}
