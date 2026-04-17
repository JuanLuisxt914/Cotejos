package com.cotejador.app.core.cotejo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParametrosCotejo {
    private final double toleranciaGramos;
    private final Map<Long, String> nombresPartidos;

    public ParametrosCotejo(double toleranciaGramos) {
        this(toleranciaGramos, Collections.<Long, String>emptyMap());
    }

    public ParametrosCotejo(double toleranciaGramos, Map<Long, String> nombresPartidos) {
        if (toleranciaGramos < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa.");
        }
        this.toleranciaGramos = toleranciaGramos;
        if (nombresPartidos == null) {
            this.nombresPartidos = Collections.emptyMap();
        } else {
            this.nombresPartidos = Collections.unmodifiableMap(new HashMap<Long, String>(nombresPartidos));
        }
    }

    public double getToleranciaGramos() {
        return toleranciaGramos;
    }

    public Map<Long, String> getNombresPartidos() {
        return nombresPartidos;
    }
}
