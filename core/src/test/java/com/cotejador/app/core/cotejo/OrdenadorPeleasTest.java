package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OrdenadorPeleasTest {
    private final OrdenadorPeleas ordenadorPeleas = new OrdenadorPeleas();

    @Test
    public void evitaRepeticionesConsecutivasCuandoExisteAlternativa() {
        Pelea pelea1 = pelea(1L, 10L, 2L, 20L);
        Pelea pelea2 = pelea(3L, 10L, 4L, 30L);
        Pelea pelea3 = pelea(5L, 40L, 6L, 50L);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(
                Arrays.asList(pelea1, pelea2, pelea3),
                new ParametrosOrdenamiento(Collections.singleton(10L), 1L)
        );

        assertFalse(compartenPartido(ordenadas.get(0), ordenadas.get(1)));
        assertMismosElementos(Arrays.asList(pelea1, pelea2, pelea3), ordenadas);
    }

    @Test
    public void siNoExisteAlternativaMantieneSalidaValida() {
        Pelea pelea1 = pelea(1L, 10L, 2L, 20L);
        Pelea pelea2 = pelea(3L, 10L, 4L, 30L);
        Pelea pelea3 = pelea(5L, 10L, 6L, 40L);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(
                Arrays.asList(pelea1, pelea2, pelea3),
                new ParametrosOrdenamiento(Collections.<Long>emptySet(), 1L)
        );

        assertEquals(3, ordenadas.size());
        assertMismosElementos(Arrays.asList(pelea1, pelea2, pelea3), ordenadas);
    }

    @Test
    public void priorizaPartidosMarcadosComoPrioritariosAlInicio() {
        Pelea noPrioritaria = pelea(1L, 10L, 2L, 20L);
        Pelea prioritaria = pelea(3L, 30L, 4L, 40L);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(
                Arrays.asList(noPrioritaria, prioritaria),
                new ParametrosOrdenamiento(Collections.singleton(30L), 1L)
        );

        assertSame(prioritaria, ordenadas.get(0));
    }

    @Test
    public void noPierdePeleasNiDuplicaElementos() {
        Pelea pelea1 = pelea(1L, 10L, 2L, 20L);
        Pelea pelea2 = pelea(3L, 30L, 4L, 40L);
        Pelea pelea3 = pelea(5L, 50L, 6L, 60L);
        List<Pelea> originales = Arrays.asList(pelea1, pelea2, pelea3);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(originales, new ParametrosOrdenamiento());

        assertEquals(originales.size(), ordenadas.size());
        assertMismosElementos(originales, ordenadas);
    }

    @Test
    public void conListaVaciaDevuelveListaVacia() {
        List<Pelea> ordenadas = ordenadorPeleas.ordenar(
                Collections.<Pelea>emptyList(),
                new ParametrosOrdenamiento()
        );

        assertTrue(ordenadas.isEmpty());
    }

    @Test
    public void conUnaSolaPeleaDevuelveEsaMismaPelea() {
        Pelea pelea = pelea(1L, 10L, 2L, 20L);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(
                Collections.singletonList(pelea),
                new ParametrosOrdenamiento()
        );

        assertEquals(1, ordenadas.size());
        assertSame(pelea, ordenadas.get(0));
    }

    @Test
    public void devuelveNuevaListaSinModificarLaOriginal() {
        Pelea pelea1 = pelea(1L, 10L, 2L, 20L);
        Pelea pelea2 = pelea(3L, 30L, 4L, 40L);
        List<Pelea> originales = Arrays.asList(pelea1, pelea2);

        List<Pelea> ordenadas = ordenadorPeleas.ordenar(originales, new ParametrosOrdenamiento());

        assertFalse(originales == ordenadas);
        assertSame(pelea1, originales.get(0));
        assertSame(pelea2, originales.get(1));
    }

    private Pelea pelea(Long gallo1Id, Long partido1Id, Long gallo2Id, Long partido2Id) {
        return new Pelea(
                new Gallo(gallo1Id, "Gallo " + gallo1Id, 1000, "", partido1Id),
                new Gallo(gallo2Id, "Gallo " + gallo2Id, 1001, "", partido2Id),
                1
        );
    }

    private void assertMismosElementos(List<Pelea> esperadas, List<Pelea> actuales) {
        Set<Pelea> esperadasSet = new HashSet<Pelea>(esperadas);
        Set<Pelea> actualesSet = new HashSet<Pelea>(actuales);
        assertEquals(esperadasSet, actualesSet);
        assertEquals(esperadas.size(), actuales.size());
    }

    private boolean compartenPartido(Pelea pelea1, Pelea pelea2) {
        return partidoEnPelea(pelea1.getGallo1().getPartidoId(), pelea2)
                || partidoEnPelea(pelea1.getGallo2().getPartidoId(), pelea2);
    }

    private boolean partidoEnPelea(Long partidoId, Pelea pelea) {
        return partidoId.equals(pelea.getGallo1().getPartidoId())
                || partidoId.equals(pelea.getGallo2().getPartidoId());
    }
}
