package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.RestriccionPartido;
import com.cotejador.app.core.model.ModoCotejo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametrosCotejo {
    private final double toleranciaGramos;
    private final Map<Long, String> nombresPartidos;
    private final List<RestriccionPartido> restriccionesPartidos;
    private final int gallosPorPartido;
    private final int gallosObligatorios;
    private final boolean ultimaRondaSoloObligatorios;
    private final ModoCotejo modoCotejo;
    private final boolean excluirObligatoriosDelCotejo;

    public ParametrosCotejo(double toleranciaGramos) {
        this(toleranciaGramos, Collections.<Long, String>emptyMap());
    }

    public ParametrosCotejo(double toleranciaGramos, Map<Long, String> nombresPartidos) {
        this(toleranciaGramos, nombresPartidos, Collections.<RestriccionPartido>emptyList());
    }

    public ParametrosCotejo(double toleranciaGramos,
                            Map<Long, String> nombresPartidos,
                            List<RestriccionPartido> restriccionesPartidos) {
        this(toleranciaGramos, nombresPartidos, restriccionesPartidos, 0, 0, false);
    }

    public ParametrosCotejo(double toleranciaGramos,
                            Map<Long, String> nombresPartidos,
                            List<RestriccionPartido> restriccionesPartidos,
                            int gallosPorPartido,
                            int gallosObligatorios,
                            boolean ultimaRondaSoloObligatorios) {
        this(toleranciaGramos, nombresPartidos, restriccionesPartidos, gallosPorPartido, gallosObligatorios, ultimaRondaSoloObligatorios, ModoCotejo.ALEATORIO, false);
    }

    public ParametrosCotejo(double toleranciaGramos,
                            Map<Long, String> nombresPartidos,
                            List<RestriccionPartido> restriccionesPartidos,
                            int gallosPorPartido,
                            int gallosObligatorios,
                            boolean ultimaRondaSoloObligatorios,
                            ModoCotejo modoCotejo,
                            boolean excluirObligatoriosDelCotejo) {
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
        if (gallosPorPartido < 0) {
            throw new IllegalArgumentException("La cantidad de gallos por partido no puede ser negativa.");
        }
        if (gallosObligatorios < 0) {
            throw new IllegalArgumentException("La cantidad de gallos obligatorios no puede ser negativa.");
        }
        if (gallosObligatorios > gallosPorPartido && gallosPorPartido > 0) {
            throw new IllegalArgumentException("Los gallos obligatorios no pueden superar el total por partido.");
        }
        this.gallosPorPartido = gallosPorPartido;
        this.gallosObligatorios = gallosObligatorios;
        this.ultimaRondaSoloObligatorios = ultimaRondaSoloObligatorios;
        this.modoCotejo = modoCotejo == null ? ModoCotejo.ALEATORIO : modoCotejo;
        this.excluirObligatoriosDelCotejo = excluirObligatoriosDelCotejo;
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

    public int getGallosPorPartido() {
        return gallosPorPartido;
    }

    public int getGallosObligatorios() {
        return gallosObligatorios;
    }

    public boolean isUltimaRondaSoloObligatorios() {
        return ultimaRondaSoloObligatorios;
    }

    public ModoCotejo getModoCotejo() {
        return modoCotejo;
    }

    public boolean isExcluirObligatoriosDelCotejo() {
        return excluirObligatoriosDelCotejo;
    }

    public boolean tieneReglaDeObligatorios() {
        return gallosPorPartido > 0 && gallosObligatorios > 0;
    }

    public boolean esModoRondas() {
        return ModoCotejo.RONDAS.equals(modoCotejo);
    }
}
