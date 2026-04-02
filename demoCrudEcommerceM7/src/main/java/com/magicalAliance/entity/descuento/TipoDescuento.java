package com.magicalAliance.entity.descuento;

public enum TipoDescuento {
    PORCENTAJE("Porcentaje (%)"),
    MONTO_FIJO("Monto Fijo ($)");

    private final String label;

    TipoDescuento(String label) { this.label = label; }

    public String getLabel() { return label; }
}