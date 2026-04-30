package com.cotejador.app.core.model;

public final class EntradaGallo {
    private EntradaGallo() {
    }

    public static int calcularNumeroEntrada(Gallo gallo, int gallosPorPartido) {
        if (gallo == null || gallosPorPartido <= 0 || gallo.getOrdenRegistro() <= 0) {
            return 0;
        }
        return ((gallo.getOrdenRegistro() - 1) / gallosPorPartido) + 1;
    }

    public static String formatearEntrada(Gallo gallo, int gallosPorPartido) {
        return formatearEntrada(gallo, gallosPorPartido, true);
    }

    public static String formatearEntrada(Gallo gallo, int gallosPorPartido, boolean mostrarEntrada) {
        if (!mostrarEntrada) {
            return "-";
        }
        int entrada = calcularNumeroEntrada(gallo, gallosPorPartido);
        return entrada <= 0 ? "-" : "E" + entrada;
    }

    public static String formatearPartidoConEntrada(Gallo gallo, String nombrePartido, int gallosPorPartido) {
        return formatearPartidoConEntrada(gallo, nombrePartido, gallosPorPartido, true);
    }

    public static String formatearPartidoConEntrada(Gallo gallo, String nombrePartido, int gallosPorPartido, boolean mostrarEntrada) {
        String partido = nombrePartido == null || nombrePartido.trim().isEmpty() ? "-" : nombrePartido.trim();
        String entrada = formatearEntrada(gallo, gallosPorPartido, mostrarEntrada);
        return "-".equals(entrada) ? partido : entrada + " - " + partido;
    }
}
