package com.cotejador.app.core.cotejo;

import com.cotejador.app.core.model.Gallo;

public class Pelea {
    private final Gallo gallo1;
    private final Gallo gallo2;
    private final double diferenciaPeso;

    public Pelea(Gallo gallo1, Gallo gallo2, double diferenciaPeso) {
        this.gallo1 = gallo1;
        this.gallo2 = gallo2;
        this.diferenciaPeso = diferenciaPeso;
    }

    public Gallo getGallo1() {
        return gallo1;
    }

    public Gallo getGallo2() {
        return gallo2;
    }

    public double getDiferenciaPeso() {
        return diferenciaPeso;
    }

    @Override
    public String toString() {
        return gallo1.getNombre() + " vs " + gallo2.getNombre() +
                " | dif: " + diferenciaPeso;
    }
}
