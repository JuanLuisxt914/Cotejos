package com.cotejador.app.data.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PdfBoxRuntimeConfig {
    private static boolean configured;

    private PdfBoxRuntimeConfig() {
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }

        try {
            Path cacheDir = Paths.get(System.getProperty("user.dir"), "target", "pdfbox-cache");
            Files.createDirectories(cacheDir);
            System.setProperty("user.home", cacheDir.toAbsolutePath().normalize().toString());
            System.setProperty("pdfbox.fontcache", cacheDir.resolve(".pdfbox.cache").toAbsolutePath().normalize().toString());
            Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
            Logger.getLogger("org.apache.fontbox").setLevel(Level.SEVERE);
            Logger.getLogger("org.apache.pdfbox.pdmodel.font.FileSystemFontProvider").setLevel(Level.SEVERE);
        } catch (Exception ignored) {
            // If cache configuration fails, PDFBox will fall back to its default behavior.
        }

        configured = true;
    }
}
