package com.cotejador.app.core.model;

public class PeleaGuardada {
    private Long id;
    private Long cotejoId;
    private int orden;
    private Long gallo1Id;
    private Long gallo2Id;
    private double diferenciaPeso;
    private int ronda;

    public PeleaGuardada() {
    }

    public PeleaGuardada(Long id, Long cotejoId, int orden, Long gallo1Id, Long gallo2Id, double diferenciaPeso) {
        this(id, cotejoId, orden, gallo1Id, gallo2Id, diferenciaPeso, 1);
    }

    public PeleaGuardada(Long id, Long cotejoId, int orden, Long gallo1Id, Long gallo2Id, double diferenciaPeso, int ronda) {
        this.id = id;
        this.cotejoId = cotejoId;
        this.orden = orden;
        this.gallo1Id = gallo1Id;
        this.gallo2Id = gallo2Id;
        this.diferenciaPeso = diferenciaPeso;
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

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public Long getGallo1Id() {
        return gallo1Id;
    }

    public void setGallo1Id(Long gallo1Id) {
        this.gallo1Id = gallo1Id;
    }

    public Long getGallo2Id() {
        return gallo2Id;
    }

    public void setGallo2Id(Long gallo2Id) {
        this.gallo2Id = gallo2Id;
    }

    public double getDiferenciaPeso() {
        return diferenciaPeso;
    }

    public void setDiferenciaPeso(double diferenciaPeso) {
        this.diferenciaPeso = diferenciaPeso;
    }

    public int getRonda() {
        return ronda <= 0 ? 1 : ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = Math.max(1, ronda);
    }

    @Override
    public String toString() {
        return orden + ". Gallo " + gallo1Id + " vs Gallo " + gallo2Id +
                " | ronda: " + getRonda() +
                " | dif: " + diferenciaPeso;
    }
}
