package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PeleaGuardada;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CotejoRepositoryTest {
    @Test
    public void guardaListaCargaDetalleYEliminaCotejo() throws Exception {
        SQLiteConnectionFactory connectionFactory = createConnectionFactory();
        new DatabaseInitializer(connectionFactory).initialize();

        EventoRepository eventoRepository = new EventoRepository(connectionFactory);
        PartidoRepository partidoRepository = new PartidoRepository(connectionFactory);
        GalloRepository galloRepository = new GalloRepository(connectionFactory);
        CotejoRepository cotejoRepository = new CotejoRepository(connectionFactory);

        Evento evento = new Evento(null, "Evento test", "2026-04-17", "Presencial");
        eventoRepository.insertar(evento);

        Partido partido1 = new Partido(null, "Partido 1", evento.getId());
        Partido partido2 = new Partido(null, "Partido 2", evento.getId());
        partidoRepository.insertar(partido1);
        partidoRepository.insertar(partido2);

        Gallo gallo1 = new Gallo(null, 1000, "", partido1.getId());
        Gallo gallo2 = new Gallo(null, 1002, "", partido2.getId());
        Gallo gallo3 = new Gallo(null, 1100, "", partido2.getId());
        galloRepository.insertar(gallo1);
        galloRepository.insertar(gallo2);
        galloRepository.insertar(gallo3);

        CotejoGuardado cotejo = new CotejoGuardado(null, evento.getId(), 5, "2026-04-17T12:00:00");
        cotejo.setPeleas(Arrays.asList(new PeleaGuardada(null, null, 1, gallo1.getId(), gallo2.getId(), 2)));
        cotejo.setGallosSinPelea(Arrays.asList(new GalloSinPeleaGuardado(null, null, gallo3.getId())));

        cotejoRepository.guardar(cotejo);

        assertNotNull(cotejo.getId());
        List<CotejoGuardado> cotejos = cotejoRepository.listarPorEvento(evento.getId());
        assertEquals(1, cotejos.size());

        CotejoGuardado detalle = cotejoRepository.cargarDetalle(cotejo.getId());
        assertNotNull(detalle);
        assertEquals(1, detalle.getPeleas().size());
        assertEquals(1, detalle.getGallosSinPelea().size());
        assertEquals(gallo1.getId(), detalle.getPeleas().get(0).getGallo1Id());
        assertEquals(gallo3.getId(), detalle.getGallosSinPelea().get(0).getGalloId());

        cotejo.setToleranciaGramos(7);
        cotejo.setPeleas(Arrays.asList(new PeleaGuardada(null, null, 1, gallo1.getId(), gallo3.getId(), 100)));
        cotejo.setGallosSinPelea(Collections.<GalloSinPeleaGuardado>emptyList());
        cotejoRepository.actualizar(cotejo);

        CotejoGuardado detalleActualizado = cotejoRepository.cargarDetalle(cotejo.getId());
        assertNotNull(detalleActualizado);
        assertEquals(7, detalleActualizado.getToleranciaGramos(), 0.0);
        assertEquals(1, detalleActualizado.getPeleas().size());
        assertEquals(gallo3.getId(), detalleActualizado.getPeleas().get(0).getGallo2Id());
        assertTrue(detalleActualizado.getGallosSinPelea().isEmpty());

        cotejoRepository.eliminar(cotejo.getId());

        assertTrue(cotejoRepository.listarPorEvento(evento.getId()).isEmpty());
    }

    private SQLiteConnectionFactory createConnectionFactory() {
        File databaseFile = new File("target/test-cotejo-" + System.nanoTime() + ".db");
        return new SQLiteConnectionFactory("jdbc:sqlite:" + databaseFile.getAbsolutePath().replace('\\', '/'));
    }
}
