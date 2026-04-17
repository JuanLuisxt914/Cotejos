package com.cotejador.app.desktop;

import com.cotejador.app.core.cotejo.MotorCotejo;
import com.cotejador.app.core.cotejo.ParametrosCotejo;
import com.cotejador.app.core.cotejo.Pelea;
import com.cotejador.app.core.cotejo.ResultadoCotejo;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.data.sqlite.EventoRepository;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class MainController {
    private final EventoRepository eventoRepository;
    private final PartidoRepository partidoRepository;
    private final GalloRepository galloRepository;
    private final MotorCotejo motorCotejo = new MotorCotejo();

    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();
    private final ObservableList<Partido> partidos = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallos = FXCollections.observableArrayList();
    private final ObservableList<Pelea> peleasCotejo = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallosSinPelea = FXCollections.observableArrayList();

    private final ListView<Evento> eventList = new ListView<Evento>(eventos);
    private final ListView<Partido> partidoList = new ListView<Partido>(partidos);
    private final ListView<Gallo> galloList = new ListView<Gallo>(gallos);
    private final ListView<Pelea> peleasCotejoList = new ListView<Pelea>(peleasCotejo);
    private final ListView<Gallo> gallosSinPeleaList = new ListView<Gallo>(gallosSinPelea);

    private final TextField eventoNombreField = new TextField();
    private final TextField eventoFechaField = new TextField();
    private final TextField eventoModalidadField = new TextField();
    private final TextField partidoNombreField = new TextField();
    private final TextField galloNombreField = new TextField();
    private final TextField galloPesoField = new TextField();
    private final TextField galloAnilloField = new TextField();
    private final TextField toleranciaField = new TextField();
    private final Label statusLabel = new Label();

    public MainController(EventoRepository eventoRepository,
                          PartidoRepository partidoRepository,
                          GalloRepository galloRepository) {
        this.eventoRepository = eventoRepository;
        this.partidoRepository = partidoRepository;
        this.galloRepository = galloRepository;
    }

    public Parent createView() throws Exception {
        configureFields();
        configureSelectionListeners();
        loadEventos(null);

        Button crearEventoButton = new Button("Crear evento");
        Button editarEventoButton = new Button("Editar evento");
        Button eliminarEventoButton = new Button("Eliminar evento");
        Button agregarPartidoButton = new Button("Agregar partido");
        Button editarPartidoButton = new Button("Editar partido");
        Button eliminarPartidoButton = new Button("Eliminar partido");
        Button agregarGalloButton = new Button("Agregar gallo");
        Button editarGalloButton = new Button("Editar gallo");
        Button eliminarGalloButton = new Button("Eliminar gallo");
        Button cotejarButton = new Button("Cotejar");

        crearEventoButton.setOnAction(event -> crearEvento());
        editarEventoButton.setOnAction(event -> editarEvento());
        eliminarEventoButton.setOnAction(event -> eliminarEvento());
        agregarPartidoButton.setOnAction(event -> agregarPartido());
        editarPartidoButton.setOnAction(event -> editarPartido());
        eliminarPartidoButton.setOnAction(event -> eliminarPartido());
        agregarGalloButton.setOnAction(event -> agregarGallo());
        editarGalloButton.setOnAction(event -> editarGallo());
        eliminarGalloButton.setOnAction(event -> eliminarGallo());
        cotejarButton.setOnAction(event -> cotejarEventoSeleccionado());

        VBox eventBox = new VBox(6,
                new Label("Eventos"),
                eventoNombreField,
                eventoFechaField,
                eventoModalidadField,
                new HBox(6, crearEventoButton, editarEventoButton),
                eliminarEventoButton,
                eventList
        );
        VBox partidoBox = new VBox(6,
                new Label("Partidos del evento"),
                partidoNombreField,
                new HBox(6, agregarPartidoButton, editarPartidoButton),
                eliminarPartidoButton,
                partidoList
        );
        VBox galloBox = new VBox(6,
                new Label("Gallos del partido"),
                galloNombreField,
                galloPesoField,
                galloAnilloField,
                new HBox(6, agregarGalloButton, editarGalloButton),
                eliminarGalloButton,
                galloList
        );
        VBox cotejoBox = new VBox(6,
                new Label("Cotejo del evento"),
                toleranciaField,
                cotejarButton,
                new Label("Peleas generadas"),
                peleasCotejoList,
                new Label("Gallos sin pelea"),
                gallosSinPeleaList
        );

        HBox content = new HBox(10, eventBox, partidoBox, galloBox, cotejoBox);
        HBox.setHgrow(eventBox, Priority.ALWAYS);
        HBox.setHgrow(partidoBox, Priority.ALWAYS);
        HBox.setHgrow(galloBox, Priority.ALWAYS);
        HBox.setHgrow(cotejoBox, Priority.ALWAYS);
        VBox.setVgrow(eventList, Priority.ALWAYS);
        VBox.setVgrow(partidoList, Priority.ALWAYS);
        VBox.setVgrow(galloList, Priority.ALWAYS);
        VBox.setVgrow(peleasCotejoList, Priority.ALWAYS);
        VBox.setVgrow(gallosSinPeleaList, Priority.ALWAYS);

        return new VBox(10, new Label("Cotejador"), content, statusLabel);
    }

    private void configureFields() {
        eventoNombreField.setPromptText("Nombre del evento");
        eventoFechaField.setPromptText("Fecha");
        eventoModalidadField.setPromptText("Modalidad");
        partidoNombreField.setPromptText("Nombre del partido");
        galloNombreField.setPromptText("Nombre del gallo");
        galloPesoField.setPromptText("Peso en gramos");
        galloAnilloField.setPromptText("Anillo");
        toleranciaField.setPromptText("Tolerancia en gramos");
    }

    private void configureSelectionListeners() {
        eventList.getSelectionModel().selectedItemProperty().addListener((observable, oldEvent, selectedEvent) -> {
            try {
                partidos.clear();
                gallos.clear();
                clearCotejoResults();
                clearPartidoFields();
                clearGalloFields();

                if (selectedEvent != null) {
                    fillEventoFields(selectedEvent);
                    partidos.setAll(partidoRepository.listarPorEvento(selectedEvent.getId()));
                } else {
                    clearEventoFields();
                }
            } catch (Exception exception) {
                showError("Error al cargar partidos", exception);
            }
        });

        partidoList.getSelectionModel().selectedItemProperty().addListener((observable, oldPartido, selectedPartido) -> {
            try {
                gallos.clear();
                clearCotejoResults();
                clearGalloFields();

                if (selectedPartido != null) {
                    fillPartidoFields(selectedPartido);
                    gallos.setAll(galloRepository.listarPorPartido(selectedPartido.getId()));
                } else {
                    clearPartidoFields();
                }
            } catch (Exception exception) {
                showError("Error al cargar gallos", exception);
            }
        });

        galloList.getSelectionModel().selectedItemProperty().addListener((observable, oldGallo, selectedGallo) -> {
            if (selectedGallo != null) {
                fillGalloFields(selectedGallo);
            } else {
                clearGalloFields();
            }
        });
    }

    private void crearEvento() {
        try {
            Evento evento = readEventoFromFields(null);
            if (evento == null) {
                return;
            }

            eventoRepository.insertar(evento);
            loadEventos(evento.getId());
            clearCotejoResults();
            clearEventoFields();
            statusLabel.setText("Evento creado.");
        } catch (Exception exception) {
            showError("Error al crear evento", exception);
        }
    }

    private void editarEvento() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento para editar.");
                return;
            }

            Evento evento = readEventoFromFields(selectedEvent.getId());
            if (evento == null) {
                return;
            }

            eventoRepository.actualizar(evento);
            loadEventos(evento.getId());
            clearCotejoResults();
            clearEventoFields();
            statusLabel.setText("Evento actualizado.");
        } catch (Exception exception) {
            showError("Error al editar evento", exception);
        }
    }

    private void eliminarEvento() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento para eliminar.");
                return;
            }

            eventoRepository.eliminar(selectedEvent.getId());
            loadEventos(null);
            partidos.clear();
            gallos.clear();
            clearCotejoResults();
            clearEventoFields();
            clearPartidoFields();
            clearGalloFields();
            statusLabel.setText("Evento eliminado.");
        } catch (Exception exception) {
            showError("Error al eliminar evento", exception);
        }
    }

    private void agregarPartido() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento antes de agregar un partido.");
                return;
            }

            String nombre = partidoNombreField.getText().trim();
            if (nombre.isEmpty()) {
                statusLabel.setText("El nombre del partido es obligatorio.");
                return;
            }

            Partido partido = new Partido(null, nombre, selectedEvent.getId());
            partidoRepository.insertar(partido);
            loadPartidos(selectedEvent.getId(), partido.getId());
            clearCotejoResults();
            clearPartidoFields();
            statusLabel.setText("Partido agregado.");
        } catch (Exception exception) {
            showError("Error al agregar partido", exception);
        }
    }

    private void editarPartido() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento.");
                return;
            }
            if (selectedPartido == null) {
                statusLabel.setText("Selecciona un partido para editar.");
                return;
            }

            String nombre = partidoNombreField.getText().trim();
            if (nombre.isEmpty()) {
                statusLabel.setText("El nombre del partido es obligatorio.");
                return;
            }

            Partido partido = new Partido(selectedPartido.getId(), nombre, selectedEvent.getId());
            partidoRepository.actualizar(partido);
            loadPartidos(selectedEvent.getId(), partido.getId());
            clearCotejoResults();
            clearPartidoFields();
            statusLabel.setText("Partido actualizado.");
        } catch (Exception exception) {
            showError("Error al editar partido", exception);
        }
    }

    private void eliminarPartido() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento.");
                return;
            }
            if (selectedPartido == null) {
                statusLabel.setText("Selecciona un partido para eliminar.");
                return;
            }

            partidoRepository.eliminar(selectedPartido.getId());
            loadPartidos(selectedEvent.getId(), null);
            gallos.clear();
            clearCotejoResults();
            clearPartidoFields();
            clearGalloFields();
            statusLabel.setText("Partido eliminado.");
        } catch (Exception exception) {
            showError("Error al eliminar partido", exception);
        }
    }

    private void agregarGallo() {
        try {
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                statusLabel.setText("Selecciona un partido antes de agregar un gallo.");
                return;
            }

            Gallo gallo = readGalloFromFields(null, selectedPartido.getId());
            if (gallo == null) {
                return;
            }

            galloRepository.insertar(gallo);
            loadGallos(selectedPartido.getId(), gallo.getId());
            clearCotejoResults();
            clearGalloFields();
            statusLabel.setText("Gallo agregado.");
        } catch (Exception exception) {
            showError("Error al agregar gallo", exception);
        }
    }

    private void editarGallo() {
        try {
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloList.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                statusLabel.setText("Selecciona un partido.");
                return;
            }
            if (selectedGallo == null) {
                statusLabel.setText("Selecciona un gallo para editar.");
                return;
            }

            Gallo gallo = readGalloFromFields(selectedGallo.getId(), selectedPartido.getId());
            if (gallo == null) {
                return;
            }

            galloRepository.actualizar(gallo);
            loadGallos(selectedPartido.getId(), gallo.getId());
            clearCotejoResults();
            clearGalloFields();
            statusLabel.setText("Gallo actualizado.");
        } catch (Exception exception) {
            showError("Error al editar gallo", exception);
        }
    }

    private void eliminarGallo() {
        try {
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloList.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                statusLabel.setText("Selecciona un partido.");
                return;
            }
            if (selectedGallo == null) {
                statusLabel.setText("Selecciona un gallo para eliminar.");
                return;
            }

            galloRepository.eliminar(selectedGallo.getId());
            loadGallos(selectedPartido.getId(), null);
            clearCotejoResults();
            clearGalloFields();
            statusLabel.setText("Gallo eliminado.");
        } catch (Exception exception) {
            showError("Error al eliminar gallo", exception);
        }
    }

    private Evento readEventoFromFields(Long id) {
        String nombre = eventoNombreField.getText().trim();
        String fecha = eventoFechaField.getText().trim();
        String modalidad = eventoModalidadField.getText().trim();

        if (nombre.isEmpty()) {
            statusLabel.setText("El nombre del evento es obligatorio.");
            return null;
        }
        if (fecha.isEmpty()) {
            statusLabel.setText("La fecha del evento es obligatoria.");
            return null;
        }
        if (modalidad.isEmpty()) {
            statusLabel.setText("La modalidad del evento es obligatoria.");
            return null;
        }

        return new Evento(id, nombre, fecha, modalidad);
    }

    private Gallo readGalloFromFields(Long id, Long partidoId) {
        String nombre = galloNombreField.getText().trim();
        String pesoText = galloPesoField.getText().trim();
        String anillo = galloAnilloField.getText().trim();

        if (nombre.isEmpty()) {
            statusLabel.setText("El nombre del gallo es obligatorio.");
            return null;
        }
        if (pesoText.isEmpty()) {
            statusLabel.setText("El peso del gallo es obligatorio.");
            return null;
        }

        double peso;
        try {
            peso = Double.parseDouble(pesoText);
        } catch (NumberFormatException exception) {
            statusLabel.setText("El peso debe ser numerico.");
            return null;
        }

        if (peso <= 0) {
            statusLabel.setText("El peso debe ser mayor a 0.");
            return null;
        }

        return new Gallo(id, nombre, peso, anillo, partidoId);
    }

    private void cotejarEventoSeleccionado() {
        try {
            Evento selectedEvent = eventList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                statusLabel.setText("Selecciona un evento para cotejar.");
                return;
            }

            Map<Long, String> nombresPartidos = loadNombresPartidosDelEvento(selectedEvent);
            ParametrosCotejo parametros = readParametrosCotejo(nombresPartidos);
            if (parametros == null) {
                return;
            }

            ResultadoCotejo resultado = motorCotejo.cotejar(loadGallosDelEvento(selectedEvent), parametros);
            peleasCotejo.setAll(resultado.getPeleas());
            gallosSinPelea.setAll(resultado.getGallosSinPelea());
            statusLabel.setText("Cotejo generado: " + resultado.getPeleas().size() +
                    " peleas, " + resultado.getGallosSinPelea().size() + " gallos sin pelea.");
        } catch (Exception exception) {
            showError("Error al cotejar evento", exception);
        }
    }

    private ParametrosCotejo readParametrosCotejo(Map<Long, String> nombresPartidos) {
        String toleranciaText = toleranciaField.getText().trim();
        if (toleranciaText.isEmpty()) {
            statusLabel.setText("La tolerancia es obligatoria.");
            return null;
        }

        double tolerancia;
        try {
            tolerancia = Double.parseDouble(toleranciaText);
        } catch (NumberFormatException exception) {
            statusLabel.setText("La tolerancia debe ser numerica.");
            return null;
        }

        if (tolerancia < 0) {
            statusLabel.setText("La tolerancia no puede ser negativa.");
            return null;
        }

        return new ParametrosCotejo(tolerancia, nombresPartidos);
    }

    private java.util.List<Gallo> loadGallosDelEvento(Evento evento) throws Exception {
        java.util.List<Gallo> gallosDelEvento = new java.util.ArrayList<Gallo>();
        java.util.List<Partido> partidosDelEvento = partidoRepository.listarPorEvento(evento.getId());
        for (Partido partido : partidosDelEvento) {
            gallosDelEvento.addAll(galloRepository.listarPorPartido(partido.getId()));
        }
        return gallosDelEvento;
    }

    private Map<Long, String> loadNombresPartidosDelEvento(Evento evento) throws Exception {
        Map<Long, String> nombresPartidos = new HashMap<Long, String>();
        java.util.List<Partido> partidosDelEvento = partidoRepository.listarPorEvento(evento.getId());
        for (Partido partido : partidosDelEvento) {
            nombresPartidos.put(partido.getId(), partido.getNombre());
        }
        return nombresPartidos;
    }

    private void loadEventos(Long selectedId) throws Exception {
        eventos.setAll(eventoRepository.listarTodos());
        selectEventoById(selectedId);
    }

    private void loadPartidos(Long eventoId, Long selectedId) throws Exception {
        partidos.setAll(partidoRepository.listarPorEvento(eventoId));
        selectPartidoById(selectedId);
    }

    private void loadGallos(Long partidoId, Long selectedId) throws Exception {
        gallos.setAll(galloRepository.listarPorPartido(partidoId));
        selectGalloById(selectedId);
    }

    private void fillEventoFields(Evento evento) {
        eventoNombreField.setText(evento.getNombre());
        eventoFechaField.setText(evento.getFecha());
        eventoModalidadField.setText(evento.getModalidad());
    }

    private void fillPartidoFields(Partido partido) {
        partidoNombreField.setText(partido.getNombre());
    }

    private void fillGalloFields(Gallo gallo) {
        galloNombreField.setText(gallo.getNombre());
        galloPesoField.setText(String.valueOf(gallo.getPeso()));
        galloAnilloField.setText(gallo.getAnillo() == null ? "" : gallo.getAnillo());
    }

    private void clearEventoFields() {
        eventoNombreField.clear();
        eventoFechaField.clear();
        eventoModalidadField.clear();
    }

    private void clearPartidoFields() {
        partidoNombreField.clear();
    }

    private void clearGalloFields() {
        galloNombreField.clear();
        galloPesoField.clear();
        galloAnilloField.clear();
    }

    private void clearCotejoResults() {
        peleasCotejo.clear();
        gallosSinPelea.clear();
    }

    private void selectEventoById(Long id) {
        if (id == null) {
            eventList.getSelectionModel().clearSelection();
            return;
        }
        for (Evento evento : eventList.getItems()) {
            if (id.equals(evento.getId())) {
                eventList.getSelectionModel().select(evento);
                return;
            }
        }
    }

    private void selectPartidoById(Long id) {
        if (id == null) {
            partidoList.getSelectionModel().clearSelection();
            return;
        }
        for (Partido partido : partidoList.getItems()) {
            if (id.equals(partido.getId())) {
                partidoList.getSelectionModel().select(partido);
                return;
            }
        }
    }

    private void selectGalloById(Long id) {
        if (id == null) {
            galloList.getSelectionModel().clearSelection();
            return;
        }
        for (Gallo gallo : galloList.getItems()) {
            if (id.equals(gallo.getId())) {
                galloList.getSelectionModel().select(gallo);
                return;
            }
        }
    }

    private void showError(String message, Exception exception) {
        exception.printStackTrace();
        statusLabel.setText(message + ": " + exception.getMessage());
    }
}
