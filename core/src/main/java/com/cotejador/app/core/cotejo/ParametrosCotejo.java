package com.cotejador.app.core.cotejo;

public class ParametrosCotejo {
    private final double toleranciaGramos;

    public ParametrosCotejo(double toleranciaGramos) {
        if (toleranciaGramos < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa.");
        }
        this.toleranciaGramos = toleranciaGramos;
    }

    public double getToleranciaGramos() {
        return toleranciaGramos;
    }
}
