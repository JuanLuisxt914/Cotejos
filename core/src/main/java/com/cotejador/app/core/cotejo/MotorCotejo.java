package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.ModoCotejo;
import com.cotejador.app.core.model.RestriccionPartido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MotorCotejo {
    private final ComparadorNombresPartido comparadorNombresPartido = new ComparadorNombresPartido();

    public ResultadoCotejo cotejar(List<Gallo> gallos, ParametrosCotejo parametros) {
        if (gallos == null) {
            throw new IllegalArgumentException("La lista de gallos es obligatoria.");
        }
        if (parametros == null) {
            throw new IllegalArgumentException("Los parametros de cotejo son obligatorios.");
        }

        List<Gallo> candidatosOrdenados = ordenarGallosPorPeso(gallos);
        if (parametros.getModoCotejo() == ModoCotejo.RONDAS) {
            return cotejarPorRondas(candidatosOrdenados, parametros);
        }
        return cotejarAleatorio(candidatosOrdenados, parametros);
    }

    private ResultadoCotejo cotejarAleatorio(List<Gallo> candidatosOrdenados, ParametrosCotejo parametros) {
        List<Gallo> regulares = new ArrayList<Gallo>();
        List<Gallo> obligatorios = new ArrayList<Gallo>();

        for (Gallo gallo : candidatosOrdenados) {
            if (gallo.isObligatorio()) {
                obligatorios.add(gallo);
            } else {
                regulares.add(gallo);
            }
        }

        List<Pelea> peleas = new ArrayList<Pelea>();
        Set<Gallo> gallosUsados = new HashSet<Gallo>();
        Set<Long> galloIdsUsados = new HashSet<Long>();

        cotejarLista(regulares, peleas, gallosUsados, galloIdsUsados, parametros);

        if (!parametros.isExcluirObligatoriosDelCotejo() && parametros.isUltimaRondaSoloObligatorios()) {
            cotejarLista(obligatorios, peleas, gallosUsados, galloIdsUsados, parametros);
        }

        return construirResultado(candidatosOrdenados, peleas, gallosUsados, galloIdsUsados);
    }

    private ResultadoCotejo cotejarPorRondas(List<Gallo> candidatosOrdenados, ParametrosCotejo parametros) {
        List<Gallo> regulares = new ArrayList<Gallo>();
        List<Gallo> obligatorios = new ArrayList<Gallo>();

        for (Gallo gallo : candidatosOrdenados) {
            if (gallo.isObligatorio()) {
                obligatorios.add(gallo);
            } else {
                regulares.add(gallo);
            }
        }

        List<Pelea> peleas = new ArrayList<Pelea>();
        Set<Gallo> gallosUsados = new HashSet<Gallo>();
        Set<Long> galloIdsUsados = new HashSet<Long>();

        cotejarEnRondas(regulares, peleas, gallosUsados, galloIdsUsados, parametros);

        if (!parametros.isExcluirObligatoriosDelCotejo() && parametros.isUltimaRondaSoloObligatorios()) {
            cotejarEnRondas(obligatorios, peleas, gallosUsados, galloIdsUsados, parametros);
        }

        return construirResultado(candidatosOrdenados, peleas, gallosUsados, galloIdsUsados);
    }

    private void cotejarEnRondas(List<Gallo> candidatos,
                                 List<Pelea> peleas,
                                 Set<Gallo> gallosUsados,
                                 Set<Long> galloIdsUsados,
                                 ParametrosCotejo parametros) {
        Map<Long, List<Gallo>> gallosPorPartido = agruparPorPartido(candidatos);
        List<Long> partidosOrdenados = new ArrayList<Long>(gallosPorPartido.keySet());
        Collections.sort(partidosOrdenados);

        List<List<Gallo>> rondas = construirRondas(gallosPorPartido, partidosOrdenados);
        int numeroRonda = 0;
        for (List<Gallo> ronda : rondas) {
            List<Gallo> rondaMezclada = new ArrayList<Gallo>(ronda);
            Collections.shuffle(rondaMezclada, new java.util.Random(31L + numeroRonda));
            cotejarLista(rondaMezclada, peleas, gallosUsados, galloIdsUsados, parametros);
            numeroRonda++;
        }
    }

    private List<List<Gallo>> construirRondas(Map<Long, List<Gallo>> gallosPorPartido, List<Long> partidosOrdenados) {
        List<List<Gallo>> rondas = new ArrayList<List<Gallo>>();
        int maxCantidad = 0;
        for (Long partidoId : partidosOrdenados) {
            List<Gallo> gallos = gallosPorPartido.get(partidoId);
            if (gallos != null && gallos.size() > maxCantidad) {
                maxCantidad = gallos.size();
            }
        }

        for (int indiceRonda = 0; indiceRonda < maxCantidad; indiceRonda++) {
            List<Gallo> ronda = new ArrayList<Gallo>();
            for (Long partidoId : partidosOrdenados) {
                List<Gallo> gallos = gallosPorPartido.get(partidoId);
                if (gallos != null && indiceRonda < gallos.size()) {
                    ronda.add(gallos.get(indiceRonda));
                }
            }
            rondas.add(ronda);
        }

        return rondas;
    }

    private Map<Long, List<Gallo>> agruparPorPartido(List<Gallo> candidatos) {
        Map<Long, List<Gallo>> agrupados = new HashMap<Long, List<Gallo>>();
        for (Gallo gallo : candidatos) {
            Long partidoId = gallo.getPartidoId();
            if (!agrupados.containsKey(partidoId)) {
                agrupados.put(partidoId, new ArrayList<Gallo>());
            }
            agrupados.get(partidoId).add(gallo);
        }

        for (List<Gallo> gallosDelPartido : agrupados.values()) {
            Collections.shuffle(gallosDelPartido, new java.util.Random(17L + gallosDelPartido.size()));
        }
        return agrupados;
    }

    private ResultadoCotejo construirResultado(List<Gallo> candidatosOrdenados,
                                                List<Pelea> peleas,
                                                Set<Gallo> gallosUsados,
                                                Set<Long> galloIdsUsados) {
        List<Gallo> gallosSinPelea = new ArrayList<Gallo>();
        for (Gallo gallo : candidatosOrdenados) {
            if (!estaUsado(gallo, gallosUsados, galloIdsUsados)) {
                gallosSinPelea.add(gallo);
            }
        }
        return new ResultadoCotejo(peleas, gallosSinPelea);
    }

    private List<Gallo> ordenarGallosPorPeso(List<Gallo> gallos) {
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
        return candidatos;
    }

    private void cotejarLista(List<Gallo> candidatos,
                              List<Pelea> peleas,
                              Set<Gallo> gallosUsados,
                              Set<Long> galloIdsUsados,
                              ParametrosCotejo parametros) {
        for (Gallo gallo : candidatos) {
            if (estaUsado(gallo, gallosUsados, galloIdsUsados)) {
                continue;
            }

            Gallo mejorRival = buscarMejorRival(gallo, candidatos, gallosUsados, galloIdsUsados, parametros);
            if (mejorRival != null) {
                gallosUsados.add(gallo);
                gallosUsados.add(mejorRival);
                registrarIdUsado(gallo, galloIdsUsados);
                registrarIdUsado(mejorRival, galloIdsUsados);
                peleas.add(new Pelea(gallo, mejorRival, diferenciaPeso(gallo, mejorRival)));
            }
        }
    }

    private Gallo buscarMejorRival(Gallo gallo,
                                   List<Gallo> candidatos,
                                   Set<Gallo> gallosUsados,
                                   Set<Long> galloIdsUsados,
                                   ParametrosCotejo parametros) {
        Gallo mejorRival = null;
        double mejorDiferencia = Double.MAX_VALUE;

        for (Gallo rival : candidatos) {
            if (!esRivalValido(gallo, rival, gallosUsados, galloIdsUsados, parametros)) {
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
                                  Set<Long> galloIdsUsados,
                                  ParametrosCotejo parametros) {
        if (gallo == rival) {
            return false;
        }
        if (gallo.getId() != null && gallo.getId().equals(rival.getId())) {
            return false;
        }
        if (estaUsado(rival, gallosUsados, galloIdsUsados)) {
            return false;
        }
        if (gallo.getPartidoId() != null && gallo.getPartidoId().equals(rival.getPartidoId())) {
            return false;
        }
        if (nombresPartidosCoinciden(gallo, rival, parametros)) {
            return false;
        }
        if (existeRestriccionManualProhibida(gallo, rival, parametros)) {
            return false;
        }
        return diferenciaPeso(gallo, rival) <= parametros.getToleranciaGramos();
    }

    private boolean nombresPartidosCoinciden(Gallo gallo, Gallo rival, ParametrosCotejo parametros) {
        String nombrePartido = parametros.getNombresPartidos().get(gallo.getPartidoId());
        String nombreRival = parametros.getNombresPartidos().get(rival.getPartidoId());
        return comparadorNombresPartido.coinciden(nombrePartido, nombreRival);
    }

    private boolean existeRestriccionManualProhibida(Gallo gallo, Gallo rival, ParametrosCotejo parametros) {
        for (RestriccionPartido restriccion : parametros.getRestriccionesPartidos()) {
            if (RestriccionPartido.TIPO_PROHIBIDO.equals(restriccion.getTipo())
                    && restriccion.aplicaEntre(gallo.getPartidoId(), rival.getPartidoId())) {
                return true;
            }
        }
        return false;
    }

    private boolean estaUsado(Gallo gallo, Set<Gallo> gallosUsados, Set<Long> galloIdsUsados) {
        return gallosUsados.contains(gallo) ||
                (gallo.getId() != null && galloIdsUsados.contains(gallo.getId()));
    }

    private void registrarIdUsado(Gallo gallo, Set<Long> galloIdsUsados) {
        if (gallo.getId() != null) {
            galloIdsUsados.add(gallo.getId());
        }
    }

    private double diferenciaPeso(Gallo gallo1, Gallo gallo2) {
        return Math.abs(gallo1.getPeso() - gallo2.getPeso());
    }

}
