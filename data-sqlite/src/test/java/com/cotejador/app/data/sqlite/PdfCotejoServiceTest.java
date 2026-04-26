package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PeleaGuardada;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PdfCotejoServiceTest {
    @Test
    public void exportaPdfDeCotejoGuardado() throws Exception {
        Path databasePath = Paths.get("target", "test-pdf-cotejo-" + System.nanoTime() + ".db");
        String jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath().normalize().toString().replace('\\', '/');
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory(jdbcUrl);

        new DatabaseInitializer(connectionFactory).initialize();

        EventoRepository eventoRepository = new EventoRepository(connectionFactory);
        PartidoRepository partidoRepository = new PartidoRepository(connectionFactory);
        GalloRepository galloRepository = new GalloRepository(connectionFactory);
        CotejoRepository cotejoRepository = new CotejoRepository(connectionFactory);
        PdfCotejoService pdfCotejoService = new PdfCotejoService(
                cotejoRepository,
                eventoRepository,
                partidoRepository,
                galloRepository);

        Evento evento = new Evento(null, "Festival PDF", "2026-04-17", "Ligue 1");
        eventoRepository.insertar(evento);

        Partido partido1 = new Partido(null, "Partido Alfa", evento.getId());
        Partido partido2 = new Partido(null, "Partido Beta", evento.getId());
        partidoRepository.insertar(partido1);
        partidoRepository.insertar(partido2);

        Gallo gallo1 = new Gallo(null, 2500.0, "A-1", partido1.getId());
        Gallo gallo2 = new Gallo(null, 2492.5, "B-2", partido2.getId());
        Gallo gallo3 = new Gallo(null, 2400.0, "C-3", partido1.getId());
        galloRepository.insertar(gallo1);
        galloRepository.insertar(gallo2);
        galloRepository.insertar(gallo3);

        CotejoGuardado cotejo = new CotejoGuardado(null, evento.getId(), 10.0, "2026-04-17T12:00:00");
        cotejo.setPeleas(java.util.Collections.singletonList(
                new PeleaGuardada(null, null, 1, gallo1.getId(), gallo2.getId(), 7.5)
        ));
        cotejo.setGallosSinPelea(java.util.Collections.singletonList(
                new GalloSinPeleaGuardado(null, null, gallo3.getId())
        ));
        cotejoRepository.guardar(cotejo);

        Path pdfPath = Paths.get("target", "test-pdf-cotejo-" + System.nanoTime() + ".pdf");
        pdfCotejoService.exportar(cotejo.getId(), pdfPath);

        assertTrue(Files.exists(pdfPath));
        assertTrue(Files.size(pdfPath) > 0L);

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1);
        }
    }
}
