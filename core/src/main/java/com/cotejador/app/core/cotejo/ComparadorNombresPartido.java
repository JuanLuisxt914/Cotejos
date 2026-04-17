package com.cotejador.app.core.cotejo;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ComparadorNombresPartido {
    private static final Set<String> PALABRAS_COMUNES = new HashSet<String>(Arrays.asList(
            "de", "del", "la", "las", "el", "los", "y"
    ));

    public boolean coinciden(String nombre1, String nombre2) {
        String normalizado1 = normalizar(nombre1);
        String normalizado2 = normalizar(nombre2);

        if (normalizado1.isEmpty() || normalizado2.isEmpty()) {
            return false;
        }
        if (normalizado1.equals(normalizado2)) {
            return true;
        }

        Set<String> tokens1 = tokensSignificativos(normalizado1);
        Set<String> tokens2 = tokensSignificativos(normalizado2);

        for (String token : tokens1) {
            if (tokens2.contains(token)) {
                return true;
            }
        }

        return false;
    }

    public String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }

        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokensSignificativos(String normalizado) {
        Set<String> tokens = new HashSet<String>();
        if (normalizado.isEmpty()) {
            return tokens;
        }

        String[] partes = normalizado.split(" ");
        for (String parte : partes) {
            if (!parte.isEmpty() && !PALABRAS_COMUNES.contains(parte)) {
                tokens.add(parte);
            }
        }
        return tokens;
    }
}
