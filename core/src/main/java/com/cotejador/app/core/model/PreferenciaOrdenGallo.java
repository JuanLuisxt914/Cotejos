package com.cotejador.app.core.model;

public enum PreferenciaOrdenGallo {
    SIN_PREFERENCIA("Sin preferencia"),
    PRIMERA_RONDA("Primera ronda"),
    RONDA_ESPECIFICA("Ronda especifica"),
    ULTIMA_RONDA("Ultima ronda");

    private final String etiqueta;

    PreferenciaOrdenGallo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static PreferenciaOrdenGallo fromDb(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SIN_PREFERENCIA;
        }
        try {
            return PreferenciaOrdenGallo.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SIN_PREFERENCIA;
        }
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
