package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.PreferenciaOrdenGallo;
import com.cotejador.app.core.model.PreferenciaOrdenPartido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OrdenadorPeleas {
    public List<Pelea> ordenar(List<Pelea> peleas, ParametrosOrdenamiento parametros) {
        if (peleas == null) {
            throw new IllegalArgumentException("La lista de peleas es obligatoria.");
        }
        if (parametros == null) {
            parametros = new ParametrosOrdenamiento();
        }

        List<Pelea> restantes = new ArrayList<Pelea>(peleas);
        Collections.shuffle(restantes, new Random(parametros.getSemillaAleatoria()));

        List<Pelea> ordenadas = new ArrayList<Pelea>();
        Pelea peleaAnterior = null;
        int totalPeleas = restantes.size();

        while (!restantes.isEmpty()) {
            Pelea siguiente = seleccionarSiguiente(restantes, peleaAnterior, parametros, ordenadas.size(), totalPeleas);
            ordenadas.add(siguiente);
            restantes.remove(siguiente);
            peleaAnterior = siguiente;
        }

        return ordenadas;
    }

    private Pelea seleccionarSiguiente(List<Pelea> candidatas,
                                       Pelea peleaAnterior,
                                       ParametrosOrdenamiento parametros,
                                       int posicionActual,
                                       int totalPeleas) {
        Pelea mejor = null;
        int mejorPuntaje = Integer.MIN_VALUE;

        for (Pelea candidata : candidatas) {
            int puntaje = puntaje(candidata, peleaAnterior, parametros, posicionActual, totalPeleas);
            if (mejor == null || puntaje > mejorPuntaje) {
                mejor = candidata;
                mejorPuntaje = puntaje;
            }
        }

        return mejor;
    }

    private int puntaje(Pelea candidata, Pelea peleaAnterior, ParametrosOrdenamiento parametros,
                        int posicionActual, int totalPeleas) {
        int puntaje = 0;

        if (peleaAnterior == null || !compartenPartido(candidata, peleaAnterior)) {
            puntaje += 1000;
        }
        puntaje += puntajePreferenciaPartido(candidata, parametros, posicionActual, totalPeleas);
        puntaje += puntajePreferenciaGallo(candidata, parametros, posicionActual, totalPeleas);

        return puntaje;
    }

    private int puntajePreferenciaPartido(Pelea pelea, ParametrosOrdenamiento parametros,
                                          int posicionActual, int totalPeleas) {
        int puntaje = 0;
        puntaje += puntajePartido(pelea.getGallo1().getPartidoId(), parametros, posicionActual, totalPeleas);
        puntaje += puntajePartido(pelea.getGallo2().getPartidoId(), parametros, posicionActual, totalPeleas);
        return puntaje;
    }

    private int puntajePartido(Long partidoId, ParametrosOrdenamiento parametros,
                               int posicionActual, int totalPeleas) {
        PreferenciaOrdenPartido preferencia = parametros.getPreferenciasPartidos().get(partidoId);
        if (preferencia == null || preferencia == PreferenciaOrdenPartido.NORMAL) {
            return 0;
        }
        int desdeFinal = Math.max(0, totalPeleas - posicionActual - 1);
        if (preferencia == PreferenciaOrdenPartido.PRIMERAS) {
            return 200 + desdeFinal;
        }
        if (preferencia == PreferenciaOrdenPartido.ULTIMAS) {
            return -200 + posicionActual;
        }
        return 0;
    }

    private int puntajePreferenciaGallo(Pelea pelea, ParametrosOrdenamiento parametros,
                                        int posicionActual, int totalPeleas) {
        return puntajeGallo(pelea.getGallo1(), parametros, posicionActual, totalPeleas)
                + puntajeGallo(pelea.getGallo2(), parametros, posicionActual, totalPeleas);
    }

    private int puntajeGallo(Gallo gallo, ParametrosOrdenamiento parametros,
                             int posicionActual, int totalPeleas) {
        if (gallo == null || gallo.getId() == null) {
            return 0;
        }
        PreferenciaOrdenGallo preferencia = parametros.getPreferenciasGallos().get(gallo.getId());
        if (preferencia == null || preferencia == PreferenciaOrdenGallo.SIN_PREFERENCIA) {
            return 0;
        }
        if (preferencia == PreferenciaOrdenGallo.PRIMERA_RONDA) {
            return 500 + Math.max(0, totalPeleas - posicionActual - 1);
        }
        if (preferencia == PreferenciaOrdenGallo.ULTIMA_RONDA) {
            return -500 + posicionActual;
        }
        if (preferencia == PreferenciaOrdenGallo.RONDA_ESPECIFICA) {
            Integer ronda = parametros.getRondasPreferidasGallos().get(gallo.getId());
            if (ronda == null || ronda <= 0) {
                return 0;
            }
            int distancia = Math.abs((posicionActual + 1) - ronda);
            return 400 - (distancia * 80);
        }
        return 0;
    }

    private boolean compartenPartido(Pelea pelea1, Pelea pelea2) {
        Long pelea1Partido1 = pelea1.getGallo1().getPartidoId();
        Long pelea1Partido2 = pelea1.getGallo2().getPartidoId();
        Long pelea2Partido1 = pelea2.getGallo1().getPartidoId();
        Long pelea2Partido2 = pelea2.getGallo2().getPartidoId();

        return equalsLong(pelea1Partido1, pelea2Partido1)
                || equalsLong(pelea1Partido1, pelea2Partido2)
                || equalsLong(pelea1Partido2, pelea2Partido1)
                || equalsLong(pelea1Partido2, pelea2Partido2);
    }

    private boolean equalsLong(Long value1, Long value2) {
        return value1 != null && value1.equals(value2);
    }
}
