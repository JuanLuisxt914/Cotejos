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
import java.util.Random;
import java.util.Set;

public class MotorCotejo {
    private static final int INTENTOS_ALEATORIOS = 500;
    private static final long SEMILLA_ALEATORIA = 7301L;
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
        Map<Long, Integer> rondasPorGalloId = construirRondasAleatorias(candidatosOrdenados);
        List<Gallo> candidatosCotejables = new ArrayList<Gallo>();

        for (Gallo gallo : candidatosOrdenados) {
            if (parametros.isExcluirObligatoriosDelCotejo() && gallo.isObligatorio()) {
                continue;
            }
            candidatosCotejables.add(gallo);
        }

        IntentoCotejo mejorIntento = resolverMejorIntento(candidatosCotejables, parametros, 1, SEMILLA_ALEATORIA);

        return construirResultado(
                candidatosOrdenados,
                mejorIntento.peleas,
                mejorIntento.gallosUsados,
                mejorIntento.galloIdsUsados,
                rondasPorGalloId);
    }

    private ResultadoCotejo cotejarPorRondas(List<Gallo> candidatosOrdenados, ParametrosCotejo parametros) {
        List<Pelea> peleas = new ArrayList<Pelea>();
        Set<Gallo> gallosUsados = new HashSet<Gallo>();
        Set<Long> galloIdsUsados = new HashSet<Long>();
        Map<Long, Integer> rondasPorGalloId = construirRondasPorEntrada(candidatosOrdenados, parametros);
        Map<Integer, List<Gallo>> gallosPorRonda = agruparCotejablesPorRonda(candidatosOrdenados, parametros, rondasPorGalloId);
        List<Integer> rondasOrdenadas = new ArrayList<Integer>(gallosPorRonda.keySet());
        Collections.sort(rondasOrdenadas);

        long semillaRonda = SEMILLA_ALEATORIA * 3L;
        for (Integer ronda : rondasOrdenadas) {
            IntentoCotejo mejorIntento = resolverMejorIntento(
                    gallosPorRonda.get(ronda),
                    parametros,
                    ronda,
                    semillaRonda + ronda
            );
            mergearIntento(mejorIntento, peleas, gallosUsados, galloIdsUsados);
        }

        return construirResultado(candidatosOrdenados, peleas, gallosUsados, galloIdsUsados, rondasPorGalloId);
    }

    private IntentoCotejo resolverMejorIntento(List<Gallo> candidatos,
                                               ParametrosCotejo parametros,
                                               int ronda,
                                               long semillaBase) {
        IntentoCotejo mejorIntento = cotejarListaGreedy(candidatos, parametros, ronda);
        for (int intento = 0; intento < INTENTOS_ALEATORIOS; intento++) {
            IntentoCotejo intentoAleatorio = cotejarListaAleatoria(
                    candidatos,
                    parametros,
                    ronda,
                    new Random(semillaBase + intento)
            );
            if (esMejorIntento(intentoAleatorio, mejorIntento)) {
                mejorIntento = intentoAleatorio;
            }
        }
        return mejorIntento;
    }

    private void mergearIntento(IntentoCotejo intento,
                                List<Pelea> peleas,
                                Set<Gallo> gallosUsados,
                                Set<Long> galloIdsUsados) {
        peleas.addAll(intento.peleas);
        gallosUsados.addAll(intento.gallosUsados);
        galloIdsUsados.addAll(intento.galloIdsUsados);
    }

    private Map<Long, Integer> construirRondasPorEntrada(List<Gallo> candidatos, ParametrosCotejo parametros) {
        Map<EntradaKey, List<Gallo>> gallosPorEntrada = agruparPorEntrada(candidatos, parametros);
        Map<Long, Integer> rondasPorGalloId = new HashMap<Long, Integer>();
        int maxRondas = maxRondas(parametros, gallosPorEntrada);

        for (List<Gallo> entrada : gallosPorEntrada.values()) {
            List<Gallo> regulares = new ArrayList<Gallo>();
            List<Gallo> obligatorios = new ArrayList<Gallo>();
            for (Gallo gallo : entrada) {
                if (gallo.isObligatorio()) {
                    obligatorios.add(gallo);
                } else {
                    regulares.add(gallo);
                }
            }

            if (parametros.isExcluirObligatoriosDelCotejo() || parametros.isUltimaRondaSoloObligatorios()) {
                int rondasRegulares = Math.max(0, maxRondas - parametros.getGallosObligatorios());
                registrarRondasSecuenciales(regulares, rondasPorGalloId, rondasRegulares);
                for (Gallo obligatorio : obligatorios) {
                    registrarRonda(obligatorio, rondasPorGalloId, maxRondas);
                }
            } else {
                registrarRondasSecuenciales(entrada, rondasPorGalloId, maxRondas);
            }
        }
        return rondasPorGalloId;
    }

    private Map<Integer, List<Gallo>> agruparCotejablesPorRonda(List<Gallo> candidatos,
                                                                 ParametrosCotejo parametros,
                                                                 Map<Long, Integer> rondasPorGalloId) {
        Map<Integer, List<Gallo>> gallosPorRonda = new HashMap<Integer, List<Gallo>>();
        for (Gallo gallo : candidatos) {
            if (parametros.isExcluirObligatoriosDelCotejo() && gallo.isObligatorio()) {
                continue;
            }
            Integer ronda = gallo.getId() == null ? null : rondasPorGalloId.get(gallo.getId());
            if (ronda == null) {
                continue;
            }
            if (!gallosPorRonda.containsKey(ronda)) {
                gallosPorRonda.put(ronda, new ArrayList<Gallo>());
            }
            gallosPorRonda.get(ronda).add(gallo);
        }
        return gallosPorRonda;
    }

    private void registrarRondasSecuenciales(List<Gallo> gallos, Map<Long, Integer> rondasPorGalloId, int maxRondas) {
        int limite = maxRondas <= 0 ? gallos.size() : maxRondas;
        for (int index = 0; index < gallos.size(); index++) {
            int ronda = Math.min(index + 1, limite);
            registrarRonda(gallos.get(index), rondasPorGalloId, ronda);
        }
    }

    private Map<EntradaKey, List<Gallo>> agruparPorEntrada(List<Gallo> candidatos, ParametrosCotejo parametros) {
        Map<EntradaKey, List<Gallo>> agrupados = new HashMap<EntradaKey, List<Gallo>>();
        for (Gallo gallo : candidatos) {
            EntradaKey entradaKey = entradaKey(gallo, parametros);
            if (!agrupados.containsKey(entradaKey)) {
                agrupados.put(entradaKey, new ArrayList<Gallo>());
            }
            agrupados.get(entradaKey).add(gallo);
        }

        for (List<Gallo> gallosEntrada : agrupados.values()) {
            Collections.sort(gallosEntrada, new Comparator<Gallo>() {
                @Override
                public int compare(Gallo gallo1, Gallo gallo2) {
                    int ordenCompare = Integer.compare(gallo1.getOrdenRegistro(), gallo2.getOrdenRegistro());
                    if (ordenCompare != 0) {
                        return ordenCompare;
                    }
                    return compararPorPesoEId(gallo1, gallo2);
                }
            });
        }
        return agrupados;
    }

    private EntradaKey entradaKey(Gallo gallo, ParametrosCotejo parametros) {
        int gallosPorEntrada = parametros.getGallosPorPartido();
        int entrada = 1;
        if (gallosPorEntrada > 0 && gallo.getOrdenRegistro() > 0) {
            entrada = ((gallo.getOrdenRegistro() - 1) / gallosPorEntrada) + 1;
        }
        return new EntradaKey(gallo.getPartidoId(), entrada);
    }

    private int maxRondas(ParametrosCotejo parametros, Map<EntradaKey, List<Gallo>> gallosPorEntrada) {
        if (parametros.getGallosPorPartido() > 0) {
            return parametros.getGallosPorPartido();
        }
        int max = 0;
        for (List<Gallo> entrada : gallosPorEntrada.values()) {
            if (entrada.size() > max) {
                max = entrada.size();
            }
        }
        return Math.max(1, max);
    }

    private ResultadoCotejo construirResultado(List<Gallo> candidatosOrdenados,
                                                List<Pelea> peleas,
                                                Set<Gallo> gallosUsados,
                                                Set<Long> galloIdsUsados,
                                                Map<Long, Integer> rondasPorGalloId) {
        List<Gallo> gallosSinPelea = new ArrayList<Gallo>();
        for (Gallo gallo : candidatosOrdenados) {
            if (!estaUsado(gallo, gallosUsados, galloIdsUsados)) {
                gallosSinPelea.add(gallo);
            }
        }
        return new ResultadoCotejo(peleas, gallosSinPelea, rondasPorGalloId);
    }

    private Map<Long, Integer> construirRondasAleatorias(List<Gallo> candidatosOrdenados) {
        Map<Long, Integer> rondasPorGalloId = new HashMap<Long, Integer>();
        for (Gallo gallo : candidatosOrdenados) {
            registrarRonda(gallo, rondasPorGalloId, 1);
        }
        return rondasPorGalloId;
    }

    private void registrarRonda(List<Gallo> gallos, Map<Long, Integer> rondasPorGalloId, int ronda) {
        for (Gallo gallo : gallos) {
            registrarRonda(gallo, rondasPorGalloId, ronda);
        }
    }

    private void registrarRonda(Gallo gallo, Map<Long, Integer> rondasPorGalloId, int ronda) {
        if (gallo != null && gallo.getId() != null) {
            rondasPorGalloId.put(gallo.getId(), ronda);
        }
    }

    private List<Gallo> ordenarGallosPorPeso(List<Gallo> gallos) {
        List<Gallo> candidatos = new ArrayList<Gallo>(gallos);
        Collections.sort(candidatos, new Comparator<Gallo>() {
            @Override
            public int compare(Gallo gallo1, Gallo gallo2) {
                return compararPorPesoEId(gallo1, gallo2);
            }
        });
        return candidatos;
    }

    private int compararPorPesoEId(Gallo gallo1, Gallo gallo2) {
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

    private void cotejarLista(List<Gallo> candidatos,
                              List<Pelea> peleas,
                              Set<Gallo> gallosUsados,
                              Set<Long> galloIdsUsados,
                              ParametrosCotejo parametros,
                              int ronda) {
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
                peleas.add(new Pelea(gallo, mejorRival, diferenciaPeso(gallo, mejorRival), ronda));
            }
        }
    }

    private IntentoCotejo cotejarListaGreedy(List<Gallo> candidatos, ParametrosCotejo parametros, int ronda) {
        IntentoCotejo intento = new IntentoCotejo();
        cotejarLista(candidatos, intento.peleas, intento.gallosUsados, intento.galloIdsUsados, parametros, ronda);
        return intento;
    }

    private IntentoCotejo cotejarListaAleatoria(List<Gallo> candidatos,
                                                ParametrosCotejo parametros,
                                                int ronda,
                                                Random random) {
        IntentoCotejo intento = new IntentoCotejo();
        List<Gallo> candidatosMezclados = new ArrayList<Gallo>(candidatos);
        Collections.shuffle(candidatosMezclados, random);

        for (Gallo gallo : candidatosMezclados) {
            if (estaUsado(gallo, intento.gallosUsados, intento.galloIdsUsados)) {
                continue;
            }

            List<Gallo> rivalesValidos = buscarRivalesValidos(
                    gallo,
                    candidatosMezclados,
                    intento.gallosUsados,
                    intento.galloIdsUsados,
                    parametros);
            if (rivalesValidos.isEmpty()) {
                continue;
            }

            Collections.shuffle(rivalesValidos, random);
            Gallo rival = rivalesValidos.get(0);
            intento.gallosUsados.add(gallo);
            intento.gallosUsados.add(rival);
            registrarIdUsado(gallo, intento.galloIdsUsados);
            registrarIdUsado(rival, intento.galloIdsUsados);
            intento.peleas.add(new Pelea(gallo, rival, diferenciaPeso(gallo, rival), ronda));
        }

        return intento;
    }

    private List<Gallo> buscarRivalesValidos(Gallo gallo,
                                             List<Gallo> candidatos,
                                             Set<Gallo> gallosUsados,
                                             Set<Long> galloIdsUsados,
                                             ParametrosCotejo parametros) {
        List<Gallo> rivales = new ArrayList<Gallo>();
        for (Gallo rival : candidatos) {
            if (esRivalValido(gallo, rival, gallosUsados, galloIdsUsados, parametros)) {
                rivales.add(rival);
            }
        }
        return rivales;
    }

    private boolean esMejorIntento(IntentoCotejo candidato, IntentoCotejo actual) {
        if (actual == null) {
            return true;
        }
        if (candidato.peleas.size() != actual.peleas.size()) {
            return candidato.peleas.size() > actual.peleas.size();
        }
        return diferenciaTotal(candidato.peleas) < diferenciaTotal(actual.peleas);
    }

    private double diferenciaTotal(List<Pelea> peleas) {
        double total = 0.0;
        for (Pelea pelea : peleas) {
            total += pelea.getDiferenciaPeso();
        }
        return total;
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

    private static class EntradaKey {
        private final Long partidoId;
        private final int entrada;

        private EntradaKey(Long partidoId, int entrada) {
            this.partidoId = partidoId;
            this.entrada = entrada;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EntradaKey)) {
                return false;
            }
            EntradaKey other = (EntradaKey) obj;
            if (entrada != other.entrada) {
                return false;
            }
            if (partidoId == null) {
                return other.partidoId == null;
            }
            return partidoId.equals(other.partidoId);
        }

        @Override
        public int hashCode() {
            int result = partidoId == null ? 0 : partidoId.hashCode();
            result = 31 * result + entrada;
            return result;
        }
    }

    private static class IntentoCotejo {
        private final List<Pelea> peleas = new ArrayList<Pelea>();
        private final Set<Gallo> gallosUsados = new HashSet<Gallo>();
        private final Set<Long> galloIdsUsados = new HashSet<Long>();
    }

}
