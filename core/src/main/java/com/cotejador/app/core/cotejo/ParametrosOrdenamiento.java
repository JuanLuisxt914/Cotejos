package com.cotejador.app.core.cotejo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ParametrosOrdenamiento {
    private final Set<Long> partidosPrioritarios;
    private final long semillaAleatoria;

    public ParametrosOrdenamiento() {
        this(Collections.<Long>emptySet());
    }

    public ParametrosOrdenamiento(Set<Long> partidosPrioritarios) {
        this(partidosPrioritarios, 1L);
    }

    public ParametrosOrdenamiento(Set<Long> partidosPrioritarios, long semillaAleatoria) {
        if (partidosPrioritarios == null) {
            this.partidosPrioritarios = Collections.emptySet();
        } else {
            this.partidosPrioritarios = Collections.unmodifiableSet(new HashSet<Long>(partidosPrioritarios));
        }
        this.semillaAleatoria = semillaAleatoria;
    }

    public Set<Long> getPartidosPrioritarios() {
        return partidosPrioritarios;
    }

    public long getSemillaAleatoria() {
        return semillaAleatoria;
    }
}
