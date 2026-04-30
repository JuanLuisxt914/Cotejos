package com.cotejador.app.core.model;

public enum PreferenciaOrdenPartido {
    NORMAL("Normal"),
    PRIMERAS("Primeras"),
    ULTIMAS("Ultimas");

    private final String etiqueta;

    PreferenciaOrdenPartido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static PreferenciaOrdenPartido fromDb(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NORMAL;
        }
        try {
            return PreferenciaOrdenPartido.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
