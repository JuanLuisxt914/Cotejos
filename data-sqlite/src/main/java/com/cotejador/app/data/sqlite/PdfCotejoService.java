package com.cotejador.app.data.sqlite;

import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PeleaGuardada;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfCotejoService {
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 40f;
    private static final float TITLE_SIZE = 18f;
    private static final float HEADER_SIZE = 11f;
    private static final float FIGHT_TITLE_SIZE = 13f;
    private static final float TEXT_SIZE = 9.5f;
    private static final float SMALL_SIZE = 8.5f;
    private static final float LINE_HEIGHT = 13f;
    private static final float FIGHT_BORDER = 0.8f;
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        System.setProperty("pdfbox.fontcache",
                java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "pdfbox.cache").toString());
    }

    private final CotejoRepository cotejoRepository;
    private final EventoRepository eventoRepository;
    private final PartidoRepository partidoRepository;
    private final GalloRepository galloRepository;

    public PdfCotejoService(CotejoRepository cotejoRepository,
                            EventoRepository eventoRepository,
                            PartidoRepository partidoRepository,
                            GalloRepository galloRepository) {
        this.cotejoRepository = cotejoRepository;
        this.eventoRepository = eventoRepository;
        this.partidoRepository = partidoRepository;
        this.galloRepository = galloRepository;
    }

    public void exportar(Long cotejoId, Path destino) throws Exception {
        if (cotejoId == null) {
            throw new IllegalArgumentException("El cotejo es obligatorio.");
        }
        if (destino == null) {
            throw new IllegalArgumentException("El destino es obligatorio.");
        }

        CotejoGuardado cotejo = cotejoRepository.cargarDetalle(cotejoId);
        if (cotejo == null) {
            throw new IllegalArgumentException("No se encontro el cotejo guardado.");
        }

        Evento evento = eventoRepository.buscarPorId(cotejo.getEventoId());
        if (evento == null) {
            throw new IllegalArgumentException("No se encontro el evento del cotejo.");
        }

        Map<Long, Partido> partidosPorId = indexarPartidos(partidoRepository.listarPorEvento(evento.getId()));

        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document);

            float pageWidth = PAGE_SIZE.getWidth();
            float contentWidth = pageWidth - (MARGIN * 2f);
            float colWidth = contentWidth / 2f;

            writer.writeCenteredTitle(safe(evento.getNombre()), PDType1Font.HELVETICA_BOLD, TITLE_SIZE);
            writer.blankLine();

            writer.writeKeyValueTable(new String[][] {
                {"Modalidad:", safe(evento.getModalidad())},
                {"Fecha del cotejo:", formatFecha(cotejo.getFechaGeneracion())},
                {"Tolerancia:", formatNumero(cotejo.getToleranciaGramos()) + " g"}
            });

            writer.writeHorizontalLine();
            writer.writeCenteredTitle("Peleas generadas", PDType1Font.HELVETICA_BOLD, FIGHT_TITLE_SIZE);
            writer.blankLine();
            if (cotejo.getPeleas().isEmpty()) {
                writer.writeParagraph("Sin peleas generadas.");
            }

            for (PeleaGuardada pelea : cotejo.getPeleas()) {
                writer.writeFightBlock(pelea, partidosPorId);
            }

            writer.writeHorizontalLine();
            writer.blankLine();
            writer.writeCenteredTitle("Gallos sin pelea", PDType1Font.HELVETICA_BOLD, FIGHT_TITLE_SIZE);
            writer.blankLine();

            if (cotejo.getGallosSinPelea().isEmpty()) {
                writer.writeParagraph("Sin gallos sin pelea.");
            } else {
                for (GalloSinPeleaGuardado galloSinPelea : cotejo.getGallosSinPelea()) {
                    writer.writeUnpairedGallo(galloSinPelea, partidosPorId);
                }
            }

            writer.finish();
            document.save(destino.toFile());
        }
    }

    private Map<Long, Partido> indexarPartidos(List<Partido> partidos) {
        Map<Long, Partido> partidosPorId = new HashMap<Long, Partido>();
        for (Partido partido : partidos) {
            partidosPorId.put(partido.getId(), partido);
        }
        return partidosPorId;
    }

    private String formatFecha(String fechaGeneracion) {
        if (fechaGeneracion == null || fechaGeneracion.trim().isEmpty()) {
            return "-";
        }

        try {
            LocalDateTime fecha = LocalDateTime.parse(fechaGeneracion, INPUT_DATE);
            return fecha.format(OUTPUT_DATE);
        } catch (DateTimeParseException exception) {
            return fechaGeneracion;
        }
    }

    private String formatNumero(double numero) {
        if (numero == Math.rint(numero)) {
            return String.valueOf((long) numero);
        }
        return String.valueOf(numero);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String describePartido(Long partidoId, Map<Long, Partido> partidosPorId) throws Exception {
        if (partidoId == null) {
            return "-";
        }
        Partido partido = partidosPorId.get(partidoId);
        if (partido != null) {
            return safe(partido.getNombre());
        }
        Partido partidoFallback = partidoRepository.buscarPorId(partidoId);
        return partidoFallback == null ? "Partido " + partidoId : safe(partidoFallback.getNombre());
    }

    private Gallo loadGallo(Long galloId) throws Exception {
        return galloRepository.buscarPorId(galloId);
    }

    private String describeGallo(Gallo gallo, Map<Long, Partido> partidosPorId) throws Exception {
        if (gallo == null) {
            return "Gallo";
        }

        String partido = describePartido(gallo.getPartidoId(), partidosPorId);
        return safe(gallo.getNombre()) +
                " | Partido: " + partido +
                " | Peso: " + formatNumero(gallo.getPeso()) + " g" +
                " | Anillo: " + safe(gallo.getAnillo());
    }

    private final class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float cursorY;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            startPage();
        }

        private void finish() throws IOException {
            closeContentStream();
        }

        private void writeTitle(String text) throws IOException {
            writeWrapped(text, PDType1Font.HELVETICA_BOLD, TITLE_SIZE, 18f);
            blankLine();
        }

        private void writeSectionTitle(String text) throws IOException {
            blankLine();
            writeWrapped(text, PDType1Font.HELVETICA_BOLD, SUBTITLE_SIZE, 15f);
            writeHorizontalRule();
        }

        private void writeMeta(String text) throws IOException {
            writeWrapped(text, PDType1Font.HELVETICA, TEXT_SIZE, LINE_HEIGHT);
        }

        private void writeParagraph(String text) throws IOException {
            writeWrapped(text, PDType1Font.HELVETICA, TEXT_SIZE, LINE_HEIGHT);
            blankLine();
        }

        private void writeFightBlock(PeleaGuardada pelea, Map<Long, Partido> partidosPorId) throws Exception {
            Gallo gallo1 = loadGallo(pelea.getGallo1Id());
            Gallo gallo2 = loadGallo(pelea.getGallo2Id());
            ensureBlockSpace(7);
            writeLine("Orden: " + pelea.getOrden(), PDType1Font.HELVETICA_BOLD, TEXT_SIZE);
            writeLine("Partido 1: " + describePartido(gallo1 == null ? null : gallo1.getPartidoId(), partidosPorId),
                    PDType1Font.HELVETICA, TEXT_SIZE);
            writeLine("Gallo 1: " + describeGallo(gallo1, partidosPorId), PDType1Font.HELVETICA, TEXT_SIZE);
            writeLine("Partido 2: " + describePartido(gallo2 == null ? null : gallo2.getPartidoId(), partidosPorId),
                    PDType1Font.HELVETICA, TEXT_SIZE);
            writeLine("Gallo 2: " + describeGallo(gallo2, partidosPorId), PDType1Font.HELVETICA, TEXT_SIZE);
            writeLine("Diferencia de peso: " + formatNumero(pelea.getDiferenciaPeso()) + " g",
                    PDType1Font.HELVETICA, TEXT_SIZE);
            blankLine();
        }

        private void writeUnpairedGallo(GalloSinPeleaGuardado galloSinPelea,
                                        Map<Long, Partido> partidosPorId) throws Exception {
            Gallo gallo = loadGallo(galloSinPelea.getGalloId());
            if (gallo == null) {
                writeLine("Gallo " + galloSinPelea.getGalloId(), PDType1Font.HELVETICA, TEXT_SIZE);
                return;
            }

            writeLine("Gallo: " + describeGallo(gallo, partidosPorId), PDType1Font.HELVETICA, TEXT_SIZE);
        }

        private void writeHorizontalRule() throws IOException {
            ensureSpace(LINE_HEIGHT);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN, cursorY - 2f);
            contentStream.lineTo(PAGE_SIZE.getWidth() - MARGIN, cursorY - 2f);
            contentStream.stroke();
            cursorY -= 8f;
        }

        private void writeWrapped(String text, PDFont font, float fontSize, float leading) throws IOException {
            String safeText = text == null ? "" : text;
            List<String> lines = TextWrapper.wrap(safeText, font, fontSize, PAGE_SIZE.getWidth() - (MARGIN * 2f));
            if (lines.isEmpty()) {
                lines.add("");
            }
            for (String line : lines) {
                writeLine(line, font, fontSize);
                cursorY -= (leading - LINE_HEIGHT);
            }
        }

        private void writeLine(String text, PDFont font, float fontSize) throws IOException {
            ensureSpace(LINE_HEIGHT);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, cursorY);
            contentStream.showText(text == null ? "" : text);
            contentStream.endText();
            cursorY -= LINE_HEIGHT;
        }

        private void blankLine() throws IOException {
            ensureSpace(LINE_HEIGHT);
            cursorY -= LINE_HEIGHT;
        }

        private void ensureBlockSpace(int lineCount) throws IOException {
            float needed = lineCount * LINE_HEIGHT;
            if (cursorY - needed < MARGIN) {
                startPage();
            }
        }

        private void ensureSpace(float height) throws IOException {
            if (cursorY - height < MARGIN) {
                startPage();
            }
        }

        private void startPage() throws IOException {
            closeContentStream();
            page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = PAGE_SIZE.getHeight() - MARGIN;
        }

        private void closeContentStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }

    private static final class TextWrapper {
        private static List<String> wrap(String text, PDFont font, float fontSize, float width) throws IOException {
            java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
            String[] paragraphs = text.split("\\r?\\n");
            for (String paragraph : paragraphs) {
                if (paragraph.trim().isEmpty()) {
                    lines.add("");
                    continue;
                }

                StringBuilder currentLine = new StringBuilder();
                String[] words = paragraph.trim().split("\\s+");
                for (String word : words) {
                    String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                    if (font.getStringWidth(candidate) / 1000f * fontSize <= width) {
                        currentLine.setLength(0);
                        currentLine.append(candidate);
                    } else {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                        }
                        currentLine.setLength(0);
                        currentLine.append(word);
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
            }
            return lines;
        }
    }
}
