package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.PreferenciaOrdenGallo;
import com.cotejador.app.core.model.PreferenciaOrdenPartido;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParametrosOrdenamiento {
    private final Set<Long> partidosPrioritarios;
    private final Map<Long, PreferenciaOrdenPartido> preferenciasPartidos;
    private final Map<Long, PreferenciaOrdenGallo> preferenciasGallos;
    private final Map<Long, Integer> rondasPreferidasGallos;
    private final long semillaAleatoria;

    public ParametrosOrdenamiento() {
        this(Collections.<Long>emptySet());
    }

    public ParametrosOrdenamiento(Set<Long> partidosPrioritarios) {
        this(partidosPrioritarios, 1L);
    }

    public ParametrosOrdenamiento(Set<Long> partidosPrioritarios, long semillaAleatoria) {
        this(toPreferenciasPartidos(partidosPrioritarios),
                Collections.<Long, PreferenciaOrdenGallo>emptyMap(),
                Collections.<Long, Integer>emptyMap(),
                semillaAleatoria);
    }

    public ParametrosOrdenamiento(Map<Long, PreferenciaOrdenPartido> preferenciasPartidos,
                                  Map<Long, PreferenciaOrdenGallo> preferenciasGallos,
                                  Map<Long, Integer> rondasPreferidasGallos) {
        this(preferenciasPartidos, preferenciasGallos, rondasPreferidasGallos, 1L);
    }

    public ParametrosOrdenamiento(Map<Long, PreferenciaOrdenPartido> preferenciasPartidos,
                                  Map<Long, PreferenciaOrdenGallo> preferenciasGallos,
                                  Map<Long, Integer> rondasPreferidasGallos,
                                  long semillaAleatoria) {
        this.preferenciasPartidos = immutablePreferenciasPartidos(preferenciasPartidos);
        this.preferenciasGallos = immutablePreferenciasGallos(preferenciasGallos);
        this.rondasPreferidasGallos = immutableRondas(rondasPreferidasGallos);
        this.partidosPrioritarios = Collections.unmodifiableSet(partidosPorPreferencia(
                this.preferenciasPartidos, PreferenciaOrdenPartido.PRIMERAS));
        this.semillaAleatoria = semillaAleatoria;
    }

    private static Map<Long, PreferenciaOrdenPartido> toPreferenciasPartidos(Set<Long> partidosPrioritarios) {
        Map<Long, PreferenciaOrdenPartido> preferencias = new HashMap<Long, PreferenciaOrdenPartido>();
        if (partidosPrioritarios == null) {
            return preferencias;
        }
        for (Long partidoId : partidosPrioritarios) {
            if (partidoId != null) {
                preferencias.put(partidoId, PreferenciaOrdenPartido.PRIMERAS);
            }
        }
        return preferencias;
    }

    private static Map<Long, PreferenciaOrdenPartido> immutablePreferenciasPartidos(
            Map<Long, PreferenciaOrdenPartido> preferencias) {
        Map<Long, PreferenciaOrdenPartido> copy = new HashMap<Long, PreferenciaOrdenPartido>();
        if (preferencias != null) {
            for (Map.Entry<Long, PreferenciaOrdenPartido> entry : preferencias.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getValue() != PreferenciaOrdenPartido.NORMAL) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<Long, PreferenciaOrdenGallo> immutablePreferenciasGallos(
            Map<Long, PreferenciaOrdenGallo> preferencias) {
        Map<Long, PreferenciaOrdenGallo> copy = new HashMap<Long, PreferenciaOrdenGallo>();
        if (preferencias != null) {
            for (Map.Entry<Long, PreferenciaOrdenGallo> entry : preferencias.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getValue() != PreferenciaOrdenGallo.SIN_PREFERENCIA) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<Long, Integer> immutableRondas(Map<Long, Integer> rondas) {
        Map<Long, Integer> copy = new HashMap<Long, Integer>();
        if (rondas != null) {
            for (Map.Entry<Long, Integer> entry : rondas.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Set<Long> partidosPorPreferencia(Map<Long, PreferenciaOrdenPartido> preferencias,
                                                    PreferenciaOrdenPartido preferencia) {
        Set<Long> partidos = new HashSet<Long>();
        for (Map.Entry<Long, PreferenciaOrdenPartido> entry : preferencias.entrySet()) {
            if (preferencia == entry.getValue()) {
                partidos.add(entry.getKey());
            }
        }
        return partidos;
    }

    public Set<Long> getPartidosPrioritarios() {
        return partidosPrioritarios;
    }

    public Map<Long, PreferenciaOrdenPartido> getPreferenciasPartidos() {
        return preferenciasPartidos;
    }

    public Map<Long, PreferenciaOrdenGallo> getPreferenciasGallos() {
        return preferenciasGallos;
    }

    public Map<Long, Integer> getRondasPreferidasGallos() {
        return rondasPreferidasGallos;
    }

    public long getSemillaAleatoria() {
        return semillaAleatoria;
    }
}
