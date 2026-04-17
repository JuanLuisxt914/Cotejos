package com.cotejador.app.core.model;

public class GalloSinPeleaGuardado {
    private Long id;
    private Long cotejoId;
    private Long galloId;

    public GalloSinPeleaGuardado() {
    }

    public GalloSinPeleaGuardado(Long id, Long cotejoId, Long galloId) {
        this.id = id;
        this.cotejoId = cotejoId;
        this.galloId = galloId;
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

    @Override
    public String toString() {
        return "Gallo " + galloId;
    }
}
