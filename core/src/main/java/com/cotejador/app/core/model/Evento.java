package com.cotejador.app.core.model;

public class Evento {
    private Long id;
    private String nombre;
    private String fecha;
    private String modalidad;

    public Evento() {
    }

    public Evento(Long id, String nombre, String fecha, String modalidad) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.modalidad = modalidad;
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

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    @Override
    public String toString() {
        return id + " - " + nombre + " | " + fecha + " | " + modalidad;
    }
}
