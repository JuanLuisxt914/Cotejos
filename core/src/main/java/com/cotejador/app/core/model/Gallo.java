package com.cotejador.app.core.model;

public class Gallo {
    private Long id;
    private String nombre;
    private double peso;
    private String anillo;
    private Long partidoId;

    public Gallo() {
    }

    public Gallo(Long id, String nombre, double peso, String anillo, Long partidoId) {
        this.id = id;
        this.nombre = nombre;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    @Override
    public String toString() {
        return id + " - " + nombre + " | " + peso + " | " + anillo;
    }
}
