package com.cotejador.app.core.model;

public class Gallo {
    private Long id;
    private double peso;
    private String anillo;
    private Long partidoId;
    private boolean obligatorio;
    private int ordenRegistro;
    private PreferenciaOrdenGallo preferenciaOrden;
    private Integer rondaPreferida;

    public Gallo() {
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId) {
        this.id = id;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
        this.obligatorio = false;
        this.ordenRegistro = 0;
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId, boolean obligatorio) {
        this.id = id;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
        this.obligatorio = obligatorio;
        this.ordenRegistro = 0;
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId, boolean obligatorio, int ordenRegistro) {
        this(id, peso, anillo, partidoId, obligatorio, ordenRegistro,
                PreferenciaOrdenGallo.SIN_PREFERENCIA, null);
    }

    public Gallo(Long id, double peso, String anillo, Long partidoId, boolean obligatorio, int ordenRegistro,
                 PreferenciaOrdenGallo preferenciaOrden, Integer rondaPreferida) {
        this.id = id;
        this.peso = peso;
        this.anillo = anillo;
        this.partidoId = partidoId;
        this.obligatorio = obligatorio;
        this.ordenRegistro = ordenRegistro;
        this.preferenciaOrden = preferenciaOrden == null ? PreferenciaOrdenGallo.SIN_PREFERENCIA : preferenciaOrden;
        this.rondaPreferida = rondaPreferida;
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

    public int getOrdenRegistro() {
        return ordenRegistro;
    }

    public void setOrdenRegistro(int ordenRegistro) {
        this.ordenRegistro = ordenRegistro;
    }

    public PreferenciaOrdenGallo getPreferenciaOrden() {
        return preferenciaOrden == null ? PreferenciaOrdenGallo.SIN_PREFERENCIA : preferenciaOrden;
    }

    public void setPreferenciaOrden(PreferenciaOrdenGallo preferenciaOrden) {
        this.preferenciaOrden = preferenciaOrden == null ? PreferenciaOrdenGallo.SIN_PREFERENCIA : preferenciaOrden;
    }

    public Integer getRondaPreferida() {
        return rondaPreferida;
    }

    public void setRondaPreferida(Integer rondaPreferida) {
        this.rondaPreferida = rondaPreferida;
    }

    @Override
    public String toString() {
        return "ID " + id + " | " + peso + " | " + anillo +
                " | orden " + ordenRegistro +
                " | " + getPreferenciaOrden().getEtiqueta() +
                (obligatorio ? " | obligatorio" : "");
    }
}
