package com.magicalAliance.entity.descuento;

public enum AlcanceDescuento {
    GLOBAL("Global (todos los productos)"),
    CATEGORIA("Por Categoría"),
    SUBCATEGORIA("Por Subcategoría"),
    PRODUCTO("Producto Específico");

    private final String label;

    AlcanceDescuento(String label) { this.label = label; }

    public String getLabel() { return label; }
}
