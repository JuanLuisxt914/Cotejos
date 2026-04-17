package com.cotejador.app.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CotejoGuardado {
    private Long id;
    private Long eventoId;
    private double toleranciaGramos;
    private String fechaGeneracion;
    private List<PeleaGuardada> peleas = new ArrayList<PeleaGuardada>();
    private List<GalloSinPeleaGuardado> gallosSinPelea = new ArrayList<GalloSinPeleaGuardado>();

    public CotejoGuardado() {
    }

    public CotejoGuardado(Long id, Long eventoId, double toleranciaGramos, String fechaGeneracion) {
        this.id = id;
        this.eventoId = eventoId;
        this.toleranciaGramos = toleranciaGramos;
        this.fechaGeneracion = fechaGeneracion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public double getToleranciaGramos() {
        return toleranciaGramos;
    }

    public void setToleranciaGramos(double toleranciaGramos) {
        this.toleranciaGramos = toleranciaGramos;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(String fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public List<PeleaGuardada> getPeleas() {
        return Collections.unmodifiableList(peleas);
    }

    public void setPeleas(List<PeleaGuardada> peleas) {
        this.peleas = new ArrayList<PeleaGuardada>(peleas);
    }

    public List<GalloSinPeleaGuardado> getGallosSinPelea() {
        return Collections.unmodifiableList(gallosSinPelea);
    }

    public void setGallosSinPelea(List<GalloSinPeleaGuardado> gallosSinPelea) {
        this.gallosSinPelea = new ArrayList<GalloSinPeleaGuardado>(gallosSinPelea);
    }

    @Override
    public String toString() {
        return id + " | " + fechaGeneracion + " | tol: " + toleranciaGramos;
    }
}
