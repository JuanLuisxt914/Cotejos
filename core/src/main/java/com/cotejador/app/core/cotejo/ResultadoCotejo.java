package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultadoCotejo {
    private final List<Pelea> peleas;
    private final List<Gallo> gallosSinPelea;
    private final Map<Long, Integer> rondasPorGalloId;

    public ResultadoCotejo(List<Pelea> peleas, List<Gallo> gallosSinPelea) {
        this(peleas, gallosSinPelea, Collections.<Long, Integer>emptyMap());
    }

    public ResultadoCotejo(List<Pelea> peleas, List<Gallo> gallosSinPelea, Map<Long, Integer> rondasPorGalloId) {
        this.peleas = Collections.unmodifiableList(new ArrayList<Pelea>(peleas));
        this.gallosSinPelea = Collections.unmodifiableList(new ArrayList<Gallo>(gallosSinPelea));
        this.rondasPorGalloId = Collections.unmodifiableMap(new HashMap<Long, Integer>(rondasPorGalloId));
    }

    public List<Pelea> getPeleas() {
        return peleas;
    }

    public List<Gallo> getGallosSinPelea() {
        return gallosSinPelea;
    }

    public Map<Long, Integer> getRondasPorGalloId() {
        return rondasPorGalloId;
    }
}
