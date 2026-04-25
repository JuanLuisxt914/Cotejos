package com.cotejador.app.core.model;

public class Evento {
    private Long id;
    private String nombre;
    private String fecha;
    private String modalidad;
    private int gallosPorPartido;
    private int gallosObligatorios;
    private boolean ultimaRondaSoloObligatorios;
    private ModoCotejo modoCotejo;
    private boolean excluirObligatoriosDelCotejo;

    public Evento() {
    }

    public Evento(Long id, String nombre, String fecha, String modalidad) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.modalidad = modalidad;
        this.gallosPorPartido = 0;
        this.gallosObligatorios = 0;
        this.ultimaRondaSoloObligatorios = false;
        this.modoCotejo = ModoCotejo.ALEATORIO;
        this.excluirObligatoriosDelCotejo = false;
    }

    public Evento(Long id,
                  String nombre,
                  String fecha,
                  String modalidad,
                  int gallosPorPartido,
                  int gallosObligatorios,
                  boolean ultimaRondaSoloObligatorios) {
        this(id, nombre, fecha, gallosPorPartido, gallosObligatorios, ultimaRondaSoloObligatorios);
        this.modalidad = modalidad;
    }

    public Evento(Long id,
                  String nombre,
                  String fecha,
                  int gallosPorPartido,
                  int gallosObligatorios,
                  boolean ultimaRondaSoloObligatorios) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.gallosPorPartido = gallosPorPartido;
        this.gallosObligatorios = gallosObligatorios;
        this.ultimaRondaSoloObligatorios = ultimaRondaSoloObligatorios;
        this.modalidad = null;
        this.modoCotejo = ModoCotejo.ALEATORIO;
        this.excluirObligatoriosDelCotejo = false;
    }

    public Evento(Long id,
                  String nombre,
                  String fecha,
                  String modalidad,
                  int gallosPorPartido,
                  int gallosObligatorios,
                  boolean ultimaRondaSoloObligatorios,
                  ModoCotejo modoCotejo,
                  boolean excluirObligatoriosDelCotejo) {
        this(id, nombre, fecha, modalidad, gallosPorPartido, gallosObligatorios, ultimaRondaSoloObligatorios);
        this.modoCotejo = modoCotejo == null ? ModoCotejo.ALEATORIO : modoCotejo;
        this.excluirObligatoriosDelCotejo = excluirObligatoriosDelCotejo;
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
        String resumen = construirResumenModalidad();
        if (resumen != null) {
            return resumen;
        }
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public int getGallosPorPartido() {
        return gallosPorPartido;
    }

    public void setGallosPorPartido(int gallosPorPartido) {
        this.gallosPorPartido = gallosPorPartido;
    }

    public int getGallosObligatorios() {
        return gallosObligatorios;
    }

    public void setGallosObligatorios(int gallosObligatorios) {
        this.gallosObligatorios = gallosObligatorios;
    }

    public boolean isUltimaRondaSoloObligatorios() {
        return ultimaRondaSoloObligatorios;
    }

    public void setUltimaRondaSoloObligatorios(boolean ultimaRondaSoloObligatorios) {
        this.ultimaRondaSoloObligatorios = ultimaRondaSoloObligatorios;
    }

    public ModoCotejo getModoCotejo() {
        return modoCotejo == null ? ModoCotejo.ALEATORIO : modoCotejo;
    }

    public void setModoCotejo(ModoCotejo modoCotejo) {
        this.modoCotejo = modoCotejo == null ? ModoCotejo.ALEATORIO : modoCotejo;
    }

    public boolean isExcluirObligatoriosDelCotejo() {
        return excluirObligatoriosDelCotejo;
    }

    public void setExcluirObligatoriosDelCotejo(boolean excluirObligatoriosDelCotejo) {
        this.excluirObligatoriosDelCotejo = excluirObligatoriosDelCotejo;
    }

    public int getGallosCotejadosPorPartido() {
        return Math.max(0, gallosPorPartido - gallosObligatorios);
    }

    private String construirResumenModalidad() {
        if (gallosPorPartido <= 0) {
            return modalidad != null && !modalidad.trim().isEmpty() ? modalidad : null;
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append(gallosPorPartido)
                .append(gallosPorPartido == 1 ? " gallo por partido" : " gallos por partido");

        if (gallosObligatorios > 0) {
            resumen.append(", ")
                    .append(gallosObligatorios)
                    .append(gallosObligatorios == 1 ? " obligatorio" : " obligatorios");
        }

        return resumen.toString();
    }

    @Override
    public String toString() {
        return id + " - " + nombre + " | " + fecha + " | " + getModalidad();
    }
}
