package com.cotejador.app.desktop;

import com.cotejador.app.core.model.Evento;
import com.cotejador.app.data.sqlite.EventoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventosController {

    @FXML
    private VBox eventosRoot;

    @FXML
    private TextField eventoNombreField;

    @FXML
    private DatePicker eventoFechaPicker;

    @FXML
    private Spinner<Integer> gallosPorPartidoSpinner;

    @FXML
    private Spinner<Integer> gallosObligatoriosSpinner;

    @FXML
    private ListView<Evento> eventoList;

    @FXML
    private Button crearEventoButton;

    @FXML
    private Button editarEventoButton;

    @FXML
    private Button eliminarEventoButton;

    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ISO_LOCAL_DATE;

    private EventoRepository eventoRepository;
    private ShellController shellController;

    public void setShellController(ShellController shellController) {
        this.shellController = shellController;
    }

    public void setRepositories(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
        eventoList.setItems(eventos);
        configureSelectionListener();
    }

    @FXML
    public void initialize() {
        if (eventosRoot != null && !eventosRoot.getStyleClass().contains("events-view")) {
            eventosRoot.getStyleClass().add("events-view");
        }
        eventoNombreField.setPromptText("Nombre del evento");
        eventoFechaPicker.setPromptText("Fecha");
        gallosPorPartidoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        gallosObligatoriosSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99, 0));
        gallosPorPartidoSpinner.setEditable(true);
        gallosObligatoriosSpinner.setEditable(true);
        gallosPorPartidoSpinner.valueProperty().addListener((observable, oldValue, newValue) -> updateObligatoriosMax(newValue));
        eventoList.setCellFactory(listView -> new ListCell<Evento>() {
            private final Label titulo = new Label();
            private final Label fecha = new Label();
            private final Label detalles = new Label();
            private final VBox card = new VBox(4, titulo, fecha, detalles);

            {
                titulo.getStyleClass().add("event-card-title");
                fecha.getStyleClass().add("event-card-meta");
                detalles.getStyleClass().add("event-card-meta");
                card.getStyleClass().add("event-card");
            }

            @Override
            protected void updateItem(Evento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                titulo.setText(item.getNombre());
                fecha.setText("Fecha: " + (item.getFecha() == null ? "-" : item.getFecha()));
                detalles.setText("Gallos: " + item.getGallosPorPartido() + "  |  Obligatorios: " + item.getGallosObligatorios());
                setText(null);
                setGraphic(card);
            }
        });
        eventoList.setOnMouseClicked(this::handleEventoListMouseClicked);
    }

    private void configureSelectionListener() {
        eventoList.getSelectionModel().selectedItemProperty().addListener((observable, oldEvent, selectedEvent) -> {
            if (selectedEvent != null) {
                eventoNombreField.setText(selectedEvent.getNombre());
                eventoFechaPicker.setValue(parseFecha(selectedEvent.getFecha()));
                gallosPorPartidoSpinner.getValueFactory().setValue(selectedEvent.getGallosPorPartido());
                updateObligatoriosMax(selectedEvent.getGallosPorPartido());
                gallosObligatoriosSpinner.getValueFactory().setValue(selectedEvent.getGallosObligatorios());
            } else {
                clearFields();
            }
        });
    }

    @FXML
    public void crearEvento() {
        try {
            Evento evento = readEventoFromFields(null);
            if (evento == null) return;

            eventoRepository.insertar(evento);
            loadEventos(evento.getId());
            clearFields();
            shellController.setStatus("Evento creado.");
        } catch (Exception e) {
            shellController.showError("Error al crear evento", e);
        }
    }

    @FXML
    public void editarEvento() {
        try {
            Evento selectedEvent = eventoList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                shellController.setStatus("Selecciona un evento para editar.");
                return;
            }

            Evento evento = readEventoFromFields(selectedEvent.getId());
            if (evento == null) return;

            eventoRepository.actualizar(evento);
            loadEventos(evento.getId());
            clearFields();
            shellController.setStatus("Evento actualizado.");
        } catch (Exception e) {
            shellController.showError("Error al editar evento", e);
        }
    }

    @FXML
    public void eliminarEvento() {
        try {
            Evento selectedEvent = eventoList.getSelectionModel().getSelectedItem();
            if (selectedEvent == null) {
                shellController.setStatus("Selecciona un evento para eliminar.");
                return;
            }

            eventoRepository.eliminar(selectedEvent.getId());
            loadEventos(null);
            clearFields();
            shellController.onEventoSeleccionado(null);
            shellController.setStatus("Evento eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar evento", e);
        }
    }

    public void loadEventos() {
        loadEventos(null);
    }

    public void loadEventos(Long selectedId) {
        try {
            List<Evento> eventosOrdenados = new ArrayList<>(eventoRepository.listarTodos());
            eventosOrdenados.sort(Comparator.comparing(Evento::getId, Comparator.nullsLast(Comparator.reverseOrder())));
            eventos.setAll(eventosOrdenados);
            selectEventoById(selectedId);
        } catch (Exception e) {
            shellController.showError("Error al cargar eventos", e);
        }
    }

    private Evento readEventoFromFields(Long id) {
        String nombre = eventoNombreField.getText().trim();
        LocalDate fechaSeleccionada = eventoFechaPicker.getValue();
        Integer gallosPorPartido = gallosPorPartidoSpinner.getValue();
        Integer gallosObligatorios = gallosObligatoriosSpinner.getValue();

        if (nombre.isEmpty()) {
            shellController.setStatus("El nombre del evento es obligatorio.");
            return null;
        }
        if (fechaSeleccionada == null) {
            shellController.setStatus("La fecha del evento es obligatoria.");
            return null;
        }

        if (gallosPorPartido == null || gallosPorPartido <= 0) {
            shellController.setStatus("La cantidad de gallos por partido debe ser mayor a 0.");
            return null;
        }

        if (gallosObligatorios == null || gallosObligatorios < 0) {
            shellController.setStatus("La cantidad de gallos obligatorios no puede ser negativa.");
            return null;
        }

        if (gallosObligatorios > gallosPorPartido) {
            shellController.setStatus("Los gallos obligatorios no pueden superar el total por partido.");
            return null;
        }

        Evento evento = new Evento(id, nombre, fechaSeleccionada.format(FECHA_FORMATO), null);
        evento.setGallosPorPartido(gallosPorPartido);
        evento.setGallosObligatorios(gallosObligatorios);
        return evento;
    }

    private void selectEventoById(Long id) {
        if (id == null) {
            eventoList.getSelectionModel().clearSelection();
            return;
        }
        for (Evento evento : eventos) {
            if (id.equals(evento.getId())) {
                eventoList.getSelectionModel().select(evento);
                shellController.onEventoSeleccionado(evento);
                return;
            }
        }
    }

    private void clearFields() {
        eventoNombreField.clear();
        eventoFechaPicker.setValue(null);
        gallosPorPartidoSpinner.getValueFactory().setValue(1);
        gallosObligatoriosSpinner.getValueFactory().setValue(0);
    }

    private void updateObligatoriosMax(Integer gallosPorPartido) {
        SpinnerValueFactory<Integer> valueFactory = gallosObligatoriosSpinner.getValueFactory();
        if (valueFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
            SpinnerValueFactory.IntegerSpinnerValueFactory integerValueFactory =
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) valueFactory;
            int max = gallosPorPartido == null ? 0 : Math.max(0, gallosPorPartido);
            integerValueFactory.setMin(0);
            integerValueFactory.setMax(max);

            Integer current = gallosObligatoriosSpinner.getValue();
            if (current == null || current > max) {
                gallosObligatoriosSpinner.getValueFactory().setValue(0);
            }
        }
    }

    private void handleEventoListMouseClicked(Event event) {
        if (!(event instanceof javafx.scene.input.MouseEvent)) {
            return;
        }

        javafx.scene.input.MouseEvent mouseEvent = (javafx.scene.input.MouseEvent) event;
        if (mouseEvent.getButton() != MouseButton.PRIMARY || mouseEvent.getClickCount() < 2) {
            return;
        }

        Evento selectedEvent = eventoList.getSelectionModel().getSelectedItem();
        if (selectedEvent != null) {
            shellController.onEventoSeleccionado(selectedEvent);
        }
    }

    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(fecha.trim(), FECHA_FORMATO);
        } catch (Exception e) {
            return null;
        }
    }
}
