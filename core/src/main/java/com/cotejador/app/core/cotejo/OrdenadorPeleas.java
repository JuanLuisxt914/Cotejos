package com.cotejador.app.core.cotejo;

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

        while (!restantes.isEmpty()) {
            Pelea siguiente = seleccionarSiguiente(restantes, peleaAnterior, parametros);
            ordenadas.add(siguiente);
            restantes.remove(siguiente);
            peleaAnterior = siguiente;
        }

        return ordenadas;
    }

    private Pelea seleccionarSiguiente(List<Pelea> candidatas,
                                       Pelea peleaAnterior,
                                       ParametrosOrdenamiento parametros) {
        Pelea mejor = null;
        int mejorPuntaje = Integer.MIN_VALUE;

        for (Pelea candidata : candidatas) {
            int puntaje = puntaje(candidata, peleaAnterior, parametros);
            if (mejor == null || puntaje > mejorPuntaje) {
                mejor = candidata;
                mejorPuntaje = puntaje;
            }
        }

        return mejor;
    }

    private int puntaje(Pelea candidata, Pelea peleaAnterior, ParametrosOrdenamiento parametros) {
        int puntaje = 0;

        if (peleaAnterior == null || !compartenPartido(candidata, peleaAnterior)) {
            puntaje += 1000;
        }
        if (involucraPartidoPrioritario(candidata, parametros)) {
            puntaje += 100;
        }

        return puntaje;
    }

    private boolean involucraPartidoPrioritario(Pelea pelea, ParametrosOrdenamiento parametros) {
        return parametros.getPartidosPrioritarios().contains(pelea.getGallo1().getPartidoId())
                || parametros.getPartidosPrioritarios().contains(pelea.getGallo2().getPartidoId());
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
