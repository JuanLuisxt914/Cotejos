package com.cotejador.app.core.model;

public class Partido {
    private Long id;
    private String nombre;
    private Long eventoId;
    private PreferenciaOrdenPartido preferenciaOrden;

    public Partido() {
    }

    public Partido(Long id, String nombre, Long eventoId) {
        this(id, nombre, eventoId, PreferenciaOrdenPartido.NORMAL);
    }

    public Partido(Long id, String nombre, Long eventoId, PreferenciaOrdenPartido preferenciaOrden) {
        this.id = id;
        this.nombre = nombre;
        this.eventoId = eventoId;
        this.preferenciaOrden = preferenciaOrden == null ? PreferenciaOrdenPartido.NORMAL : preferenciaOrden;
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

    public PreferenciaOrdenPartido getPreferenciaOrden() {
        return preferenciaOrden == null ? PreferenciaOrdenPartido.NORMAL : preferenciaOrden;
    }

    public void setPreferenciaOrden(PreferenciaOrdenPartido preferenciaOrden) {
        this.preferenciaOrden = preferenciaOrden == null ? PreferenciaOrdenPartido.NORMAL : preferenciaOrden;
    }

    @Override
    public String toString() {
        return id + " - " + nombre + " | " + getPreferenciaOrden().getEtiqueta();
    }
}
