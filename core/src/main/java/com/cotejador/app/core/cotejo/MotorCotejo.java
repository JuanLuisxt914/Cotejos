package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MotorCotejo {
    public ResultadoCotejo cotejar(List<Gallo> gallos, ParametrosCotejo parametros) {
        if (gallos == null) {
            throw new IllegalArgumentException("La lista de gallos es obligatoria.");
        }
        if (parametros == null) {
            throw new IllegalArgumentException("Los parametros de cotejo son obligatorios.");
        }

        List<Gallo> candidatos = new ArrayList<Gallo>(gallos);
        Collections.sort(candidatos, new Comparator<Gallo>() {
            @Override
            public int compare(Gallo gallo1, Gallo gallo2) {
                int pesoCompare = Double.compare(gallo1.getPeso(), gallo2.getPeso());
                if (pesoCompare != 0) {
                    return pesoCompare;
                }
                Long id1 = gallo1.getId();
                Long id2 = gallo2.getId();
                if (id1 == null && id2 == null) {
                    return 0;
                }
                if (id1 == null) {
                    return -1;
                }
                if (id2 == null) {
                    return 1;
                }
                return id1.compareTo(id2);
            }
        });

        List<Pelea> peleas = new ArrayList<Pelea>();
        Set<Gallo> gallosUsados = new HashSet<Gallo>();

        for (Gallo gallo : candidatos) {
            if (gallosUsados.contains(gallo)) {
                continue;
            }

            Gallo mejorRival = buscarMejorRival(gallo, candidatos, gallosUsados, parametros);
            if (mejorRival != null) {
                gallosUsados.add(gallo);
                gallosUsados.add(mejorRival);
                peleas.add(new Pelea(gallo, mejorRival, diferenciaPeso(gallo, mejorRival)));
            }
        }

        List<Gallo> gallosSinPelea = new ArrayList<Gallo>();
        for (Gallo gallo : candidatos) {
            if (!gallosUsados.contains(gallo)) {
                gallosSinPelea.add(gallo);
            }
        }

        return new ResultadoCotejo(peleas, gallosSinPelea);
    }

    private Gallo buscarMejorRival(Gallo gallo,
                                   List<Gallo> candidatos,
                                   Set<Gallo> gallosUsados,
                                   ParametrosCotejo parametros) {
        Gallo mejorRival = null;
        double mejorDiferencia = Double.MAX_VALUE;

        for (Gallo rival : candidatos) {
            if (!esRivalValido(gallo, rival, gallosUsados, parametros)) {
                continue;
            }

            double diferencia = diferenciaPeso(gallo, rival);
            if (diferencia < mejorDiferencia) {
                mejorDiferencia = diferencia;
                mejorRival = rival;
            }
        }

        return mejorRival;
    }

    private boolean esRivalValido(Gallo gallo,
                                  Gallo rival,
                                  Set<Gallo> gallosUsados,
                                  ParametrosCotejo parametros) {
        if (gallo == rival) {
            return false;
        }
        if (gallo.getId() != null && gallo.getId().equals(rival.getId())) {
            return false;
        }
        if (gallosUsados.contains(rival)) {
            return false;
        }
        if (gallo.getPartidoId() != null && gallo.getPartidoId().equals(rival.getPartidoId())) {
            return false;
        }
        return diferenciaPeso(gallo, rival) <= parametros.getToleranciaGramos();
    }

    private double diferenciaPeso(Gallo gallo1, Gallo gallo2) {
        return Math.abs(gallo1.getPeso() - gallo2.getPeso());
    }
}
