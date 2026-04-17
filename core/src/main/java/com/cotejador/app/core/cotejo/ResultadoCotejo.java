package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoCotejo {
    private final List<Pelea> peleas;
    private final List<Gallo> gallosSinPelea;

    public ResultadoCotejo(List<Pelea> peleas, List<Gallo> gallosSinPelea) {
        this.peleas = Collections.unmodifiableList(new ArrayList<Pelea>(peleas));
        this.gallosSinPelea = Collections.unmodifiableList(new ArrayList<Gallo>(gallosSinPelea));
    }

    public List<Pelea> getPeleas() {
        return peleas;
    }

    public List<Gallo> getGallosSinPelea() {
        return gallosSinPelea;
    }
}
