package com.cotejador.app.core.model;

public class Partido {
    private Long id;
    private String nombre;
    private Long eventoId;

    public Partido() {
    }

    public Partido(Long id, String nombre, Long eventoId) {
        this.id = id;
        this.nombre = nombre;
        this.eventoId = eventoId;
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

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}
