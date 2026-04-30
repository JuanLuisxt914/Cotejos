package com.cotejador.app.core.model;

public class GalloSinPeleaGuardado {
    private Long id;
    private Long cotejoId;
    private Long galloId;
    private int ronda;

    public GalloSinPeleaGuardado() {
    }

    public GalloSinPeleaGuardado(Long id, Long cotejoId, Long galloId) {
        this(id, cotejoId, galloId, 1);
    }

    public GalloSinPeleaGuardado(Long id, Long cotejoId, Long galloId, int ronda) {
        this.id = id;
        this.cotejoId = cotejoId;
        this.galloId = galloId;
        this.ronda = Math.max(1, ronda);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCotejoId() {
        return cotejoId;
    }

    public void setCotejoId(Long cotejoId) {
        this.cotejoId = cotejoId;
    }

    public Long getGalloId() {
        return galloId;
    }

    public void setGalloId(Long galloId) {
        this.galloId = galloId;
    }

    public int getRonda() {
        return ronda <= 0 ? 1 : ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = Math.max(1, ronda);
    }

    @Override
    public String toString() {
        return "Gallo " + galloId + " | ronda: " + getRonda();
    }
}
