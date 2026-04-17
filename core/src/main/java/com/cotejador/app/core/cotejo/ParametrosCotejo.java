package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.RestriccionPartido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametrosCotejo {
    private final double toleranciaGramos;
    private final Map<Long, String> nombresPartidos;
    private final List<RestriccionPartido> restriccionesPartidos;

    public ParametrosCotejo(double toleranciaGramos) {
        this(toleranciaGramos, Collections.<Long, String>emptyMap());
    }

    public ParametrosCotejo(double toleranciaGramos, Map<Long, String> nombresPartidos) {
        this(toleranciaGramos, nombresPartidos, Collections.<RestriccionPartido>emptyList());
    }

    public ParametrosCotejo(double toleranciaGramos,
                            Map<Long, String> nombresPartidos,
                            List<RestriccionPartido> restriccionesPartidos) {
        if (toleranciaGramos < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa.");
        }
        this.toleranciaGramos = toleranciaGramos;
        if (nombresPartidos == null) {
            this.nombresPartidos = Collections.emptyMap();
        } else {
            this.nombresPartidos = Collections.unmodifiableMap(new HashMap<Long, String>(nombresPartidos));
        }
        if (restriccionesPartidos == null) {
            this.restriccionesPartidos = Collections.emptyList();
        } else {
            this.restriccionesPartidos = Collections.unmodifiableList(
                    new ArrayList<RestriccionPartido>(restriccionesPartidos));
        }
    }

    public double getToleranciaGramos() {
        return toleranciaGramos;
    }

    public Map<Long, String> getNombresPartidos() {
        return nombresPartidos;
    }

    public List<RestriccionPartido> getRestriccionesPartidos() {
        return restriccionesPartidos;
    }
}
