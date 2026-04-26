package com.cotejador.app.core.model;

public class Gallo {
    private Long id;
    private double peso;
    private String anillo;
    private Long partidoId;
    private boolean obligatorio;

    public Gallo() {
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId) {
        this.id = id;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
        this.obligatorio = false;
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId, boolean obligatorio) {
        this.id = id;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
        this.obligatorio = obligatorio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public String getAnillo() {
        return anillo;
    }

    public void setAnillo(String anillo) {
        this.anillo = anillo;
    }

    public Long getPartidoId() {
        return partidoId;
    }

    public void setPartidoId(Long partidoId) {
        this.partidoId = partidoId;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(boolean obligatorio) {
        this.obligatorio = obligatorio;
    }

    @Override
    public String toString() {
        return "ID " + id + " | " + peso + " | " + anillo +
                (obligatorio ? " | obligatorio" : "");
    }
}
