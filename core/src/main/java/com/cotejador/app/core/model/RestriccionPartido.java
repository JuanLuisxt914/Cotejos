package com.cotejador.app.core.model;

public class RestriccionPartido {
    public static final String TIPO_PROHIBIDO = "PROHIBIDO";

    private Long id;
    private Long eventoId;
    private Long partidoOrigenId;
    private Long partidoDestinoId;
    private String tipo;

    public RestriccionPartido() {
    }

    public RestriccionPartido(Long id, Long eventoId, Long partidoOrigenId, Long partidoDestinoId, String tipo) {
        this.id = id;
        this.eventoId = eventoId;
        this.partidoOrigenId = partidoOrigenId;
        this.partidoDestinoId = partidoDestinoId;
        this.tipo = tipo;
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

    public Long getPartidoOrigenId() {
        return partidoOrigenId;
    }

    public void setPartidoOrigenId(Long partidoOrigenId) {
        this.partidoOrigenId = partidoOrigenId;
    }

    public Long getPartidoDestinoId() {
        return partidoDestinoId;
    }

    public void setPartidoDestinoId(Long partidoDestinoId) {
        this.partidoDestinoId = partidoDestinoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean aplicaEntre(Long partidoId1, Long partidoId2) {
        return (partidoOrigenId != null && partidoOrigenId.equals(partidoId1)
                && partidoDestinoId != null && partidoDestinoId.equals(partidoId2))
                || (partidoOrigenId != null && partidoOrigenId.equals(partidoId2)
                && partidoDestinoId != null && partidoDestinoId.equals(partidoId1));
    }

    @Override
    public String toString() {
        return tipo + ": " + partidoOrigenId + " - " + partidoDestinoId;
    }
}
