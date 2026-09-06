package com.opspulse.importjob.domain;

public enum ImportType {
    PRODUCTS,
    SUPPLIERS,
    ORDERS,
    INVENTORY,
    PO;

    public static ImportType parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("type is required");
        }
        try {
            return value.trim().toUpperCase(java.util.Locale.ROOT).equals("PO")
                    ? PO
                    : value.trim().toUpperCase(java.util.Locale.ROOT).equals("PRODUCTS")
                            ? PRODUCTS
                            : value.trim().toUpperCase(java.util.Locale.ROOT).equals("SUPPLIERS")
                                    ? SUPPLIERS
                                    : value.trim().toUpperCase(java.util.Locale.ROOT).equals("ORDERS")
                                            ? ORDERS
                                            : value.trim().toUpperCase(java.util.Locale.ROOT).equals("INVENTORY")
                                                    ? INVENTORY
                                                    : invalid(value);
        } catch (IllegalArgumentException exception) {
            throw exception;
        }
    }

    private static ImportType invalid(String value) {
        throw new IllegalArgumentException("Unsupported import type: " + value);
    }

    public String wireValue() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
