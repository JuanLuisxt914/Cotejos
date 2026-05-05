package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.ModoCotejo;
import com.cotejador.app.core.model.RestriccionPartido;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MotorCotejoTest {
    private final MotorCotejo motorCotejo = new MotorCotejo();

    @Test
    public void emparejaDosGallosDePartidosDistintosDentroDeLaTolerancia() {
        Gallo gallo1 = gallo(1L, "Gallo 1", 1000, 10L);
        Gallo gallo2 = gallo(2L, "Gallo 2", 1005, 20L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(gallo1, gallo2),
                new ParametrosCotejo(5)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 5);
    }

    @Test
    public void noEmparejaGallosDelMismoPartido() {
        Gallo gallo1 = gallo(1L, "Gallo 1", 1000, 10L);
        Gallo gallo2 = gallo(2L, "Gallo 2", 1001, 10L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(gallo1, gallo2),
                new ParametrosCotejo(10)
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void noReutilizaUnGalloEnMasDeUnaPelea() {
        Gallo gallo1 = gallo(1L, "Gallo 1", 1000, 10L);
        Gallo gallo2 = gallo(2L, "Gallo 2", 1001, 20L);
        Gallo gallo3 = gallo(3L, "Gallo 3", 1002, 30L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(gallo1, gallo2, gallo3),
                new ParametrosCotejo(5)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertEquals(1, resultado.getGallosSinPelea().size());
        assertNoRepeatedIds(resultado.getPeleas());
    }

    @Test
    public void dejaSinPeleaLosGallosFueraDeTolerancia() {
        Gallo gallo1 = gallo(1L, "Gallo 1", 1000, 10L);
        Gallo gallo2 = gallo(2L, "Gallo 2", 1020, 20L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(gallo1, gallo2),
                new ParametrosCotejo(10)
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void conVariosRivalesValidosEligeElDeMenorDiferenciaDePeso() {
        Gallo gallo1 = gallo(1L, "Gallo 1", 1000, 10L);
        Gallo gallo2 = gallo(2L, "Gallo 2", 1008, 20L);
        Gallo gallo3 = gallo(3L, "Gallo 3", 1002, 30L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(gallo1, gallo2, gallo3),
                new ParametrosCotejo(10)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 3L, 2);
        assertIds(resultado.getGallosSinPelea(), 2L);
    }

    @Test
    public void conListaVaciaRegresaCeroPeleasYCeroGallosSinPelea() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Collections.<Gallo>emptyList(),
                new ParametrosCotejo(10)
        );

        assertEquals(0, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
    }

    @Test
    public void conUnSoloGalloRegresaCeroPeleasYUnGalloSinPelea() {
        Gallo gallo = gallo(1L, "Gallo 1", 1000, 10L);

        ResultadoCotejo resultado = motorCotejo.cotejar(
                Collections.singletonList(gallo),
                new ParametrosCotejo(10)
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L);
    }

    @Test
    public void conVariosGallosPosiblesElResultadoEsConsistenteYNingunIdSeRepite() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L),
                        gallo(3L, "C", 1004, 30L),
                        gallo(4L, "D", 1005, 40L),
                        gallo(5L, "E", 1040, 50L)
                ),
                new ParametrosCotejo(5)
        );

        assertEquals(2, resultado.getPeleas().size());
        assertEquals(1, resultado.getGallosSinPelea().size());
        assertNoRepeatedIds(resultado.getPeleas());
        assertIds(resultado.getGallosSinPelea(), 5L);
    }

    @Test
    public void noReutilizaElMismoIdAunqueVengaEnObjetosDistintos() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L),
                        gallo(1L, "A duplicado", 1002, 30L),
                        gallo(3L, "C", 1003, 40L)
                ),
                new ParametrosCotejo(5)
        );

        assertNoRepeatedIds(resultado.getPeleas());
        assertEquals(1, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 3L);
    }

    @Test
    public void nombresDePartidosExactamenteIgualesQuedanProhibidos() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConNombres(5, nombre(10L, "Alcoyonqui"), nombre(20L, "Alcoyonqui"))
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void nombresDePartidosConCoincidenciaSignificativaQuedanProhibidos() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConNombres(5, nombre(10L, "Alcoyonqui MR"), nombre(20L, "Alcoyonqui MT"))
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void nombresDePartidosSinCoincidenciaSignificativaSePermiten() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConNombres(5, nombre(10L, "Alcoyonqui MR"), nombre(20L, "San Pedro MT"))
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 1);
    }

    @Test
    public void palabrasComunesIgnoradasNoBloqueanPorSiSolas() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConNombres(5, nombre(10L, "El Sol"), nombre(20L, "La Luna"))
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 1);
    }

    @Test
    public void siElRivalMasCercanoEstaBloqueadoPorNombreEligeElSiguienteValido() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L),
                        gallo(3L, "C", 1003, 30L)
                ),
                parametrosConNombres(
                        5,
                        nombre(10L, "Alcoyonqui MR"),
                        nombre(20L, "Alcoyonqui MT"),
                        nombre(30L, "San Pedro")
                )
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 2L, 3L, 2);
        assertIds(resultado.getGallosSinPelea(), 1L);
    }

    @Test
    public void restriccionManualProhibeEnfrentamientoAunqueElPesoSeaValido() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConRestricciones(5, restriccion(10L, 20L))
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void restriccionManualFuncionaEnAmbosSentidos() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 20L),
                        gallo(2L, "B", 1001, 10L)
                ),
                parametrosConRestricciones(5, restriccion(10L, 20L))
        );

        assertEquals(0, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L, 2L);
    }

    @Test
    public void sinRestriccionManualSigueAplicandoLaLogicaActual() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L)
                ),
                parametrosConRestricciones(5)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 1);
    }

    @Test
    public void siElRivalMasCercanoEstaProhibidoManualmenteEligeElSiguienteValido() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L),
                        gallo(3L, "C", 1003, 30L)
                ),
                parametrosConRestricciones(5, restriccion(10L, 20L))
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 2L, 3L, 2);
        assertIds(resultado.getGallosSinPelea(), 1L);
    }

    @Test
    public void enModoRondasCadaPartidoAportaUnGalloPorRondaHastaAgotarse() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A1", 1000, 10L),
                        gallo(2L, "A2", 1002, 10L),
                        gallo(3L, "A3", 1004, 10L),
                        gallo(4L, "B1", 1001, 20L),
                        gallo(5L, "B2", 1003, 20L),
                        gallo(6L, "B3", 1005, 20L)
                ),
                parametrosConModo(5, ModoCotejo.RONDAS)
        );

        assertEquals(3, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
        assertEquals(1, resultado.getPeleas().get(0).getRonda());
        assertEquals(2, resultado.getPeleas().get(1).getRonda());
        assertEquals(3, resultado.getPeleas().get(2).getRonda());
        assertNoRepeatedIds(resultado.getPeleas());
    }

    @Test
    public void enModoRondasRegistraLaRondaDelGalloSinPelea() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A1", 1000, 10L),
                        gallo(2L, "A2", 1002, 10L),
                        gallo(3L, "B1", 1001, 20L),
                        gallo(4L, "B2", 1100, 20L)
                ),
                parametrosConModo(5, ModoCotejo.RONDAS)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertEquals(2, resultado.getGallosSinPelea().size());
        boolean hayGalloSinPeleaDeSegundaRonda = false;
        for (Gallo galloSinPelea : resultado.getGallosSinPelea()) {
            Integer ronda = resultado.getRondasPorGalloId().get(galloSinPelea.getId());
            assertNotNull(ronda);
            hayGalloSinPeleaDeSegundaRonda = hayGalloSinPeleaDeSegundaRonda || ronda == 2;
        }
        assertTrue(hayGalloSinPeleaDeSegundaRonda);
        Pelea pelea = resultado.getPeleas().get(0);
        assertEquals(resultado.getRondasPorGalloId().get(pelea.getGallo1().getId()).intValue(), pelea.getRonda());
        assertEquals(resultado.getRondasPorGalloId().get(pelea.getGallo2().getId()).intValue(), pelea.getRonda());
    }

    @Test
    public void enModoRondasPuedeDejarFueraElObligatorioDelCotejo() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        galloObligatorio(1L, "A1", 1000, 10L),
                        gallo(2L, "A2", 1002, 10L),
                        gallo(3L, "B1", 1001, 20L)
                ),
                parametrosConModo(5, ModoCotejo.RONDAS, true)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L);
        assertNoRepeatedIds(resultado.getPeleas());
    }

    @Test
    public void enModoAleatorioIncluyeObligatoriosPorDefecto() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        galloObligatorio(1L, "A1", 1000, 10L),
                        gallo(2L, "B1", 1001, 20L)
                ),
                parametrosConModo(5, ModoCotejo.ALEATORIO)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 1);
        assertEquals(0, resultado.getGallosSinPelea().size());
    }

    @Test
    public void enModoAleatorioExcluyeObligatoriosSoloSiLaReglaEstaActiva() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        galloObligatorio(1L, "A1", 1000, 10L),
                        gallo(2L, "B1", 1001, 20L),
                        gallo(3L, "C1", 1002, 30L)
                ),
                parametrosConModo(5, ModoCotejo.ALEATORIO, true)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertIds(resultado.getGallosSinPelea(), 1L);
        assertNoRepeatedIds(resultado.getPeleas());
    }

    @Test
    public void enModoAleatorioUltimaRondaObligatoriaNoExcluyeObligatorios() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        galloObligatorio(1L, "A1", 1000, 10L),
                        gallo(2L, "B1", 1001, 20L)
                ),
                parametrosConUltimaRondaObligatoria(5, ModoCotejo.ALEATORIO)
        );

        assertEquals(1, resultado.getPeleas().size());
        assertPelea(resultado.getPeleas().get(0), 1L, 2L, 1);
        assertEquals(0, resultado.getGallosSinPelea().size());
    }

    @Test
    public void enModoAleatorioPriorizaMayorCantidadDePeleasSobreMejorRivalIndividual() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, "A", 1000, 10L),
                        gallo(2L, "B", 1001, 20L),
                        gallo(3L, "C", 1005, 30L),
                        gallo(4L, "D", 1006, 30L)
                ),
                parametrosConModo(5, ModoCotejo.ALEATORIO)
        );

        assertEquals(2, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
        assertNoRepeatedIds(resultado.getPeleas());
    }

    @Test
    public void enModoRondasTambienPriorizaMayorCantidadDePeleasPorRonda() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, 1000, 10L, 1),
                        gallo(2L, 1004, 20L, 1),
                        gallo(3L, 1001, 30L, 1),
                        gallo(4L, 1005, 40L, 1)
                ),
                new ParametrosCotejo(
                        4,
                        Collections.<Long, String>emptyMap(),
                        Arrays.asList(
                                restriccion(20L, 30L),
                                restriccion(20L, 40L)
                        ),
                        3,
                        0,
                        false,
                        ModoCotejo.RONDAS,
                        false
                )
        );

        assertEquals(2, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
        assertNoRepeatedIds(resultado.getPeleas());
        for (Pelea pelea : resultado.getPeleas()) {
            assertEquals(1, pelea.getRonda());
        }
    }

    @Test
    public void enModoRondasAgrupaPorEntradaYNoPorPartidoCompleto() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, 1000, 10L, 1),
                        gallo(2L, 1002, 10L, 2),
                        gallo(3L, 1004, 10L, 3),
                        gallo(4L, 1010, 10L, 4),
                        gallo(5L, 1012, 10L, 5),
                        gallo(6L, 1014, 10L, 6),
                        gallo(7L, 1001, 20L, 1),
                        gallo(8L, 1003, 20L, 2),
                        gallo(9L, 1005, 20L, 3),
                        gallo(10L, 1011, 20L, 4),
                        gallo(11L, 1013, 20L, 5),
                        gallo(12L, 1015, 20L, 6)
                ),
                parametrosConModo(5, ModoCotejo.RONDAS)
        );

        assertEquals(6, resultado.getPeleas().size());
        for (Pelea pelea : resultado.getPeleas()) {
            assertTrue("No debe generar rondas mayores a 3", pelea.getRonda() <= 3);
        }
        assertEquals(1, resultado.getRondasPorGalloId().get(1L).intValue());
        assertEquals(2, resultado.getRondasPorGalloId().get(2L).intValue());
        assertEquals(3, resultado.getRondasPorGalloId().get(3L).intValue());
        assertEquals(1, resultado.getRondasPorGalloId().get(4L).intValue());
        assertEquals(2, resultado.getRondasPorGalloId().get(5L).intValue());
        assertEquals(3, resultado.getRondasPorGalloId().get(6L).intValue());
    }

    @Test
    public void enModoRondasConObligatorioExcluidoSoloCotejaRondasRegulares() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, 1000, 10L, 1),
                        gallo(2L, 1002, 10L, 2),
                        galloObligatorio(3L, 1004, 10L, 3),
                        gallo(4L, 1001, 20L, 1),
                        gallo(5L, 1003, 20L, 2),
                        galloObligatorio(6L, 1005, 20L, 3)
                ),
                parametrosConModo(5, ModoCotejo.RONDAS, true)
        );

        assertEquals(2, resultado.getPeleas().size());
        for (Pelea pelea : resultado.getPeleas()) {
            assertTrue(pelea.getRonda() == 1 || pelea.getRonda() == 2);
        }
        assertIds(resultado.getGallosSinPelea(), 3L, 6L);
        assertEquals(3, resultado.getRondasPorGalloId().get(3L).intValue());
        assertEquals(3, resultado.getRondasPorGalloId().get(6L).intValue());
    }

    @Test
    public void enModoRondasConObligatorioUltimaRondaCotejaObligatoriosEnRondaFinal() {
        ResultadoCotejo resultado = motorCotejo.cotejar(
                Arrays.asList(
                        gallo(1L, 1000, 10L, 1),
                        gallo(2L, 1002, 10L, 2),
                        galloObligatorio(3L, 1004, 10L, 3),
                        gallo(4L, 1001, 20L, 1),
                        gallo(5L, 1003, 20L, 2),
                        galloObligatorio(6L, 1005, 20L, 3)
                ),
                parametrosConUltimaRondaObligatoria(5, ModoCotejo.RONDAS)
        );

        assertEquals(3, resultado.getPeleas().size());
        assertEquals(0, resultado.getGallosSinPelea().size());
        boolean hayPeleaRondaFinal = false;
        for (Pelea pelea : resultado.getPeleas()) {
            assertTrue("No debe generar rondas mayores a 3", pelea.getRonda() <= 3);
            hayPeleaRondaFinal = hayPeleaRondaFinal || pelea.getRonda() == 3;
        }
        assertTrue(hayPeleaRondaFinal);
        assertEquals(3, resultado.getRondasPorGalloId().get(3L).intValue());
        assertEquals(3, resultado.getRondasPorGalloId().get(6L).intValue());
    }

    private Gallo gallo(Long id, String nombre, double peso, Long partidoId) {
        return new Gallo(id, peso, "", partidoId);
    }

    private Gallo galloObligatorio(Long id, String nombre, double peso, Long partidoId) {
        return new Gallo(id, peso, "", partidoId, true);
    }

    private Gallo gallo(Long id, double peso, Long partidoId, int ordenRegistro) {
        return new Gallo(id, peso, "", partidoId, false, ordenRegistro);
    }

    private Gallo galloObligatorio(Long id, double peso, Long partidoId, int ordenRegistro) {
        return new Gallo(id, peso, "", partidoId, true, ordenRegistro);
    }

    private RestriccionPartido restriccion(Long partidoOrigenId, Long partidoDestinoId) {
        return new RestriccionPartido(
                null,
                100L,
                partidoOrigenId,
                partidoDestinoId,
                RestriccionPartido.TIPO_PROHIBIDO
        );
    }

    private NombrePartido nombre(Long partidoId, String nombre) {
        return new NombrePartido(partidoId, nombre);
    }

    private ParametrosCotejo parametrosConNombres(double tolerancia, NombrePartido... nombres) {
        Map<Long, String> nombresPartidos = new HashMap<Long, String>();
        for (NombrePartido nombre : nombres) {
            nombresPartidos.put(nombre.partidoId, nombre.nombre);
        }
        return new ParametrosCotejo(tolerancia, nombresPartidos);
    }

    private ParametrosCotejo parametrosConRestricciones(double tolerancia, RestriccionPartido... restricciones) {
        return new ParametrosCotejo(
                tolerancia,
                Collections.<Long, String>emptyMap(),
                new ArrayList<RestriccionPartido>(Arrays.asList(restricciones))
        );
    }

    private ParametrosCotejo parametrosConModo(double tolerancia, ModoCotejo modoCotejo) {
        return parametrosConModo(tolerancia, modoCotejo, false);
    }

    private ParametrosCotejo parametrosConModo(double tolerancia, ModoCotejo modoCotejo, boolean excluirObligatorios) {
        return new ParametrosCotejo(
                tolerancia,
                Collections.<Long, String>emptyMap(),
                Collections.<RestriccionPartido>emptyList(),
                3,
                1,
                false,
                modoCotejo,
                excluirObligatorios
        );
    }

    private ParametrosCotejo parametrosConUltimaRondaObligatoria(double tolerancia, ModoCotejo modoCotejo) {
        return new ParametrosCotejo(
                tolerancia,
                Collections.<Long, String>emptyMap(),
                Collections.<RestriccionPartido>emptyList(),
                3,
                1,
                true,
                modoCotejo,
                false
        );
    }

    private void assertPelea(Pelea pelea, Long gallo1Id, Long gallo2Id, double diferencia) {
        assertEquals(gallo1Id, pelea.getGallo1().getId());
        assertEquals(gallo2Id, pelea.getGallo2().getId());
        assertEquals(diferencia, pelea.getDiferenciaPeso(), 0.0001);
        assertFalse(pelea.getGallo1().getPartidoId().equals(pelea.getGallo2().getPartidoId()));
    }

    private void assertIds(List<Gallo> gallos, Long... ids) {
        assertEquals(ids.length, gallos.size());
        Set<Long> actualIds = new HashSet<Long>();
        for (Gallo gallo : gallos) {
            actualIds.add(gallo.getId());
        }
        for (Long id : ids) {
            assertTrue("No se encontro el gallo con id " + id, actualIds.contains(id));
        }
    }

    private void assertNoRepeatedIds(List<Pelea> peleas) {
        Set<Long> ids = new HashSet<Long>();
        for (Pelea pelea : peleas) {
            assertTrue("Id repetido en peleas: " + pelea.getGallo1().getId(), ids.add(pelea.getGallo1().getId()));
            assertTrue("Id repetido en peleas: " + pelea.getGallo2().getId(), ids.add(pelea.getGallo2().getId()));
            assertFalse(pelea.getGallo1().getId().equals(pelea.getGallo2().getId()));
        }
    }

    private static class NombrePartido {
        private final Long partidoId;
        private final String nombre;

        private NombrePartido(Long partidoId, String nombre) {
            this.partidoId = partidoId;
            this.nombre = nombre;
        }
    }
}
