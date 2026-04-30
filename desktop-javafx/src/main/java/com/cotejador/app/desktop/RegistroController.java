package com.cotejador.app.desktop;

import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.EntradaGallo;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PreferenciaOrdenGallo;
import com.cotejador.app.core.model.PreferenciaOrdenPartido;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class RegistroController {
    private static final RondaOption SIN_PREFERENCIA_RONDA = new RondaOption(null, "Sin preferencia");
    private static final String[] ENTRADA_ROW_STYLES = {
            "entrada-color-1",
            "entrada-color-2",
            "entrada-color-3",
            "entrada-color-4"
    };
    private static final String[] PARTIDO_ROW_STYLES = {
            "partido-color-1",
            "partido-color-2",
            "partido-color-3",
            "partido-color-4"
    };

    @FXML
    private TextField partidoNombreField;

    @FXML
    private TextField galloPesoField;

    @FXML
    private TextField galloAnilloField;

    @FXML
    private TableView<Partido> partidoTable;
    @FXML
    private TableColumn<Partido, String> partidoNombreColumn;
    @FXML
    private TableColumn<Partido, Partido> partidoOrdenColumn;
    @FXML
    private TableColumn<Partido, Partido> partidoAccionesColumn;

    @FXML
    private TableView<Gallo> galloTable;

    @FXML
    private TableColumn<Gallo, String> galloPesoColumn;

    @FXML
    private TableColumn<Gallo, String> galloAnilloColumn;

    @FXML
    private TableColumn<Gallo, String> galloEntradaColumn;

    @FXML
    private TableColumn<Gallo, Gallo> galloOrdenColumn;

    @FXML
    private TableColumn<Gallo, Gallo> galloObligatorioColumn;

    @FXML
    private TableColumn<Gallo, Gallo> galloAccionesColumn;
    @FXML
    private Label partidoSeleccionadoLabel;
    @FXML
    private HBox registroSplitBox;
    @FXML
    private VBox partidosPanel;
    @FXML
    private VBox gallosPanel;

    @FXML
    private Button agregarPartidoButton;

    @FXML
    private Button editarPartidoButton;

    @FXML
    private Button agregarGalloButton;

    @FXML
    private Button editarGalloButton;

    @FXML
    private final ObservableList<Partido> partidos = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallos = FXCollections.observableArrayList();

    private Evento eventoActual;
    private PartidoRepository partidoRepository;
    private GalloRepository galloRepository;
    private ShellController shellController;

    public void setShellController(ShellController shellController) {
        this.shellController = shellController;
    }

    public void setRepositories(PartidoRepository partidoRepository, GalloRepository galloRepository) {
        this.partidoRepository = partidoRepository;
        this.galloRepository = galloRepository;
        partidoTable.setItems(partidos);
        galloTable.setItems(gallos);
        configurePartidoTable();
        configureGalloTable();
        configureSelectionListeners();
    }

    @FXML
    public void initialize() {
        partidoNombreField.setPromptText("Nombre del partido");
        galloPesoField.setPromptText("Peso en gramos");
        galloAnilloField.setPromptText("Anillo");
        actualizarPartidoSeleccionadoLabel(null);
        configurarLayoutMitades();
    }

    private void configureSelectionListeners() {
        partidoTable.getSelectionModel().selectedItemProperty().addListener((observable, oldPartido, selectedPartido) -> {
            if (selectedPartido != null) {
                partidoNombreField.setText(selectedPartido.getNombre());
                actualizarPartidoSeleccionadoLabel(selectedPartido);
                try {
                    gallos.setAll(galloRepository.listarPorPartido(selectedPartido.getId()));
                } catch (Exception e) {
                    shellController.showError("Error al cargar gallos", e);
                }
            } else {
                partidoNombreField.clear();
                actualizarPartidoSeleccionadoLabel(null);
                gallos.clear();
            }
        });

        galloTable.getSelectionModel().selectedItemProperty().addListener((observable, oldGallo, selectedGallo) -> {
            if (selectedGallo != null) {
                galloPesoField.setText(String.valueOf(selectedGallo.getPeso()));
                galloAnilloField.setText(selectedGallo.getAnillo() == null ? "" : selectedGallo.getAnillo());
            } else {
                clearGalloFields();
            }
        });
    }

    public void onEventoCambio(Evento evento) throws Exception {
        this.eventoActual = evento;
        if (evento != null) {
            partidos.setAll(partidoRepository.listarPorEvento(evento.getId()));
            if (!partidos.isEmpty()) {
                Partido primero = partidos.get(0);
                partidoTable.getSelectionModel().select(primero);
                gallos.setAll(galloRepository.listarPorPartido(primero.getId()));
            } else {
                gallos.clear();
            }
        } else {
            partidos.clear();
            gallos.clear();
        }
        actualizarVisibilidadColumnasGallos();
        clearFields();
    }

    public void limpiar() {
        partidos.clear();
        gallos.clear();
        actualizarVisibilidadColumnasGallos();
        clearFields();
    }

    @FXML
    public void agregarPartido() {
        try {
            if (eventoActual == null) {
                shellController.setStatus("Selecciona un evento antes de agregar un partido.");
                return;
            }

            String nombre = partidoNombreField.getText().trim();
            if (nombre.isEmpty()) {
                shellController.setStatus("El nombre del partido es obligatorio.");
                return;
            }

            Partido partido = new Partido(null, nombre, eventoActual.getId(), PreferenciaOrdenPartido.NORMAL);
            partidoRepository.insertar(partido);
            loadPartidos(partido.getId());
            if (shellController != null) {
                shellController.refrescarPartidosEnPeleas();
            }
            clearPartidoFields();
            shellController.setStatus("Partido agregado.");
            focusGalloPeso();
        } catch (Exception e) {
            shellController.showError("Error al agregar partido", e);
        }
    }

    @FXML
    public void editarPartido() {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido para editar.");
                return;
            }

            String nombre = partidoNombreField.getText().trim();
            if (nombre.isEmpty()) {
                shellController.setStatus("El nombre del partido es obligatorio.");
                return;
            }

            Partido partido = new Partido(selectedPartido.getId(), nombre, eventoActual.getId(), selectedPartido.getPreferenciaOrden());
            partidoRepository.actualizar(partido);
            loadPartidos(partido.getId());
            if (shellController != null) {
                shellController.refrescarPartidosEnPeleas();
            }
            clearPartidoFields();
            shellController.setStatus("Partido actualizado.");
        } catch (Exception e) {
            shellController.showError("Error al editar partido", e);
        }
    }

    @FXML
    public void eliminarPartido() {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido para eliminar.");
                return;
            }

            partidoRepository.eliminar(selectedPartido.getId());
            loadPartidos(null);
            if (shellController != null) {
                shellController.refrescarPartidosEnPeleas();
            }
            gallos.clear();
            clearPartidoFields();
            shellController.setStatus("Partido eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar partido", e);
        }
    }

    @FXML
    public void agregarGallo() {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido antes de agregar un gallo.");
                return;
            }

            Gallo gallo = readGalloFromFields(null, selectedPartido.getId());
            if (gallo == null) return;

            galloRepository.insertar(gallo);
            loadGallos(selectedPartido.getId(), gallo.getId());
            clearGalloFields();
            shellController.setStatus("Gallo agregado.");
            focusGalloPeso();
        } catch (Exception e) {
            shellController.showError("Error al agregar gallo", e);
        }
    }

    @FXML
    public void editarGallo() {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido.");
                return;
            }
            if (selectedGallo == null) {
                shellController.setStatus("Selecciona un gallo para editar.");
                return;
            }

            Gallo gallo = readGalloFromFields(selectedGallo.getId(), selectedPartido.getId());
            if (gallo == null) return;
            gallo.setObligatorio(selectedGallo.isObligatorio());
            gallo.setOrdenRegistro(selectedGallo.getOrdenRegistro());
            gallo.setPreferenciaOrden(selectedGallo.getPreferenciaOrden());
            gallo.setRondaPreferida(selectedGallo.getRondaPreferida());

            galloRepository.actualizar(gallo);
            loadGallos(selectedPartido.getId(), gallo.getId());
            clearGalloSelection();
            clearGalloFields();
            shellController.setStatus("Gallo actualizado.");
            focusGalloPeso();
        } catch (Exception e) {
            shellController.showError("Error al editar gallo", e);
        }
    }

    @FXML
    public void eliminarGallo() {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido.");
                return;
            }
            if (selectedGallo == null) {
                shellController.setStatus("Selecciona un gallo para eliminar.");
                return;
            }

            galloRepository.eliminar(selectedGallo.getId());
            loadGallos(selectedPartido.getId(), null);
            clearGalloFields();
            shellController.setStatus("Gallo eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar gallo", e);
        }
    }

    private void loadPartidos(Long selectedId) throws Exception {
        if (eventoActual != null) {
            partidos.setAll(partidoRepository.listarPorEvento(eventoActual.getId()));
            selectPartidoById(selectedId);
        }
    }

    private void loadGallos(Long partidoId, Long selectedId) throws Exception {
        if (partidoId != null) {
            gallos.setAll(galloRepository.listarPorPartido(partidoId));
            selectGalloById(selectedId);
        }
    }

    private Gallo readGalloFromFields(Long id, Long partidoId) {
        String pesoText = galloPesoField.getText().trim();
        String anillo = galloAnilloField.getText().trim();
        boolean obligatorio = false;

        if (pesoText.isEmpty()) {
            shellController.setStatus("El peso del gallo es obligatorio.");
            return null;
        }

        double peso;
        try {
            peso = Double.parseDouble(pesoText);
        } catch (NumberFormatException e) {
            shellController.setStatus("El peso debe ser numerico.");
            return null;
        }

        if (peso <= 0) {
            shellController.setStatus("El peso debe ser mayor a 0.");
            return null;
        }

        if (!validarObligatorioDeEntrada(id)) {
            return null;
        }

        return new Gallo(id, peso, anillo, partidoId, obligatorio);
    }

    private boolean validarObligatorioDeEntrada(Long id) {
        if (eventoActual == null || eventoActual.getGallosObligatorios() <= 0) {
            return true;
        }

        int gallosPorEntrada = Math.max(1, eventoActual.getGallosPorPartido());
        int obligatoriosPorEntrada = Math.min(eventoActual.getGallosObligatorios(), gallosPorEntrada);
        int indiceActual = id == null ? gallos.size() : indexOfGallo(id);
        if (indiceActual < 0) {
            indiceActual = gallos.size();
        }

        if (id == null && !entradasCompletasTienenObligatorio(gallosPorEntrada, obligatoriosPorEntrada)) {
            return false;
        }

        return true;
    }

    private boolean entradasCompletasTienenObligatorio(int gallosPorEntrada, int obligatoriosPorEntrada) {
        int entradasCompletas = gallos.size() / gallosPorEntrada;
        for (int entrada = 0; entrada < entradasCompletas; entrada++) {
            int inicio = entrada * gallosPorEntrada;
            int fin = inicio + gallosPorEntrada;
            int obligatoriosMarcados = 0;
            for (int i = inicio; i < fin; i++) {
                if (gallos.get(i).isObligatorio()) {
                    obligatoriosMarcados++;
                }
            }
            if (obligatoriosMarcados < obligatoriosPorEntrada) {
                shellController.setStatus("Selecciona el gallo obligatorio de la entrada " + (entrada + 1) + " antes de agregar otra entrada.");
                return false;
            }
        }
        return true;
    }

    private int indexOfGallo(Long id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < gallos.size(); i++) {
            Gallo gallo = gallos.get(i);
            if (gallo.getId() != null && gallo.getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @FXML
    public void focusGalloAnillo() {
        if (galloAnilloField == null) {
            return;
        }
        Platform.runLater(() -> galloAnilloField.requestFocus());
    }

    private void focusGalloPeso() {
        if (galloPesoField == null) {
            return;
        }
        Platform.runLater(() -> galloPesoField.requestFocus());
    }

    private void selectPartidoById(Long id) {
        if (id == null) {
            partidoTable.getSelectionModel().clearSelection();
            return;
        }
        for (Partido partido : partidos) {
            if (id.equals(partido.getId())) {
                partidoTable.getSelectionModel().select(partido);
                return;
            }
        }
    }

    private void selectGalloById(Long id) {
        if (id == null) {
            galloTable.getSelectionModel().clearSelection();
            return;
        }
        for (Gallo gallo : gallos) {
            if (id.equals(gallo.getId())) {
                galloTable.getSelectionModel().select(gallo);
                return;
            }
        }
    }

    private void clearFields() {
        clearPartidoFields();
        clearGalloFields();
    }

    private void clearPartidoFields() {
        partidoNombreField.clear();
        Partido selectedPartido = partidoTable == null ? null : partidoTable.getSelectionModel().getSelectedItem();
        actualizarPartidoSeleccionadoLabel(selectedPartido);
    }

    private void clearGalloFields() {
        galloPesoField.clear();
        galloAnilloField.clear();
    }

    private void clearGalloSelection() {
        if (galloTable != null) {
            galloTable.getSelectionModel().clearSelection();
        }
    }

    private void configureGalloTable() {
        if (galloTable == null) {
            return;
        }

        galloTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        galloTable.setPlaceholder(new Label("No hay gallos registrados para el partido seleccionado."));

        if (galloPesoColumn != null) {
            galloPesoColumn.setResizable(false);
            galloPesoColumn.setMinWidth(90.0);
            galloPesoColumn.setPrefWidth(90.0);
            galloPesoColumn.setMaxWidth(90.0);
            galloPesoColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatNumero(cellData.getValue().getPeso())));
        }

        if (galloAnilloColumn != null) {
            galloAnilloColumn.setResizable(false);
            galloAnilloColumn.setMinWidth(90.0);
            galloAnilloColumn.setPrefWidth(90.0);
            galloAnilloColumn.setMaxWidth(90.0);
            galloAnilloColumn.setCellValueFactory(cellData -> {
                String anillo = cellData.getValue().getAnillo();
                return new ReadOnlyStringWrapper(anillo == null || anillo.trim().isEmpty() ? "-" : anillo.trim());
            });
        }

        if (galloEntradaColumn != null) {
            galloEntradaColumn.setResizable(false);
            galloEntradaColumn.setMinWidth(90.0);
            galloEntradaColumn.setPrefWidth(90.0);
            galloEntradaColumn.setMaxWidth(90.0);
            galloEntradaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearEntrada(cellData.getValue())));
        }

        if (galloOrdenColumn != null) {
            galloOrdenColumn.setResizable(false);
            galloOrdenColumn.setMinWidth(160.0);
            galloOrdenColumn.setPrefWidth(170.0);
            galloOrdenColumn.setMaxWidth(190.0);
            galloOrdenColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            galloOrdenColumn.setCellFactory(column -> new TableCell<Gallo, Gallo>() {
                private final ComboBox<RondaOption> rondaCombo = new ComboBox<>();
                private boolean updating;

                {
                    rondaCombo.setMaxWidth(Double.MAX_VALUE);
                    rondaCombo.setOnAction(event -> {
                        if (updating) {
                            return;
                        }
                        Gallo gallo = getItem();
                        RondaOption selected = rondaCombo.getSelectionModel().getSelectedItem();
                        if (gallo == null || selected == null) {
                            return;
                        }
                        aplicarRondaGallo(gallo, selected.getRonda());
                    });
                }

                @Override
                protected void updateItem(Gallo gallo, boolean empty) {
                    super.updateItem(gallo, empty);
                    if (empty || gallo == null) {
                        setGraphic(null);
                        return;
                    }
                    updating = true;
                    rondaCombo.setItems(FXCollections.observableArrayList(rondasDisponiblesPara(gallo)));
                    rondaCombo.getSelectionModel().select(optionForRonda(gallo.getRondaPreferida()));
                    updating = false;
                    setGraphic(rondaCombo);
                }
            });
        }

        if (galloObligatorioColumn != null) {
            galloObligatorioColumn.setResizable(false);
            galloObligatorioColumn.setMinWidth(110.0);
            galloObligatorioColumn.setPrefWidth(110.0);
            galloObligatorioColumn.setMaxWidth(110.0);
            galloObligatorioColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            galloObligatorioColumn.setCellFactory(column -> new TableCell<Gallo, Gallo>() {
                private final CheckBox obligatorioCheck = new CheckBox();

                {
                    obligatorioCheck.setOnAction(event -> {
                        Gallo gallo = getItem();
                        if (gallo == null) {
                            return;
                        }
                        aplicarObligatorioGallo(gallo, obligatorioCheck.isSelected());
                    });
                }

                @Override
                protected void updateItem(Gallo gallo, boolean empty) {
                    super.updateItem(gallo, empty);
                    if (empty || gallo == null) {
                        setGraphic(null);
                        return;
                    }
                    obligatorioCheck.setSelected(gallo.isObligatorio());
                    setGraphic(obligatorioCheck);
                }
            });
        }

        if (galloAccionesColumn != null) {
            galloAccionesColumn.setResizable(false);
            galloAccionesColumn.setMinWidth(78.0);
            galloAccionesColumn.setPrefWidth(78.0);
            galloAccionesColumn.setMaxWidth(78.0);
            galloAccionesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            galloAccionesColumn.setCellFactory(column -> new TableCell<Gallo, Gallo>() {
                private final Button eliminarButton = crearBotonEliminarIcono("Eliminar gallo");

                {
                    eliminarButton.setOnAction(event -> eliminarGalloDirecto(getItem()));
                }

                @Override
                protected void updateItem(Gallo gallo, boolean empty) {
                    super.updateItem(gallo, empty);
                    setGraphic(empty || gallo == null ? null : eliminarButton);
                }
            });
        }

        galloTable.setRowFactory(table -> new TableRow<Gallo>() {
            @Override
            protected void updateItem(Gallo gallo, boolean empty) {
                super.updateItem(gallo, empty);
                getStyleClass().removeAll(ENTRADA_ROW_STYLES);
                if (empty || gallo == null) {
                    return;
                }
                int entrada = entradaIndexFor(gallo);
                if (entrada >= 0) {
                    getStyleClass().add(ENTRADA_ROW_STYLES[entrada % ENTRADA_ROW_STYLES.length]);
                }
            }
        });

        galloTable.widthProperty().addListener((obs, oldWidth, newWidth) -> ajustarAnchoColumnasGallo());
        actualizarVisibilidadColumnasGallos();
        Platform.runLater(this::ajustarAnchoColumnasGallo);
    }

    private void ajustarAnchoColumnasGallo() {
        if (galloTable == null || galloAccionesColumn == null) {
            return;
        }
        double reserved = 24.0;
        double fixed = 0.0;
        fixed += galloPesoColumn == null ? 0.0 : galloPesoColumn.getWidth();
        fixed += galloAnilloColumn == null ? 0.0 : galloAnilloColumn.getWidth();
        fixed += galloEntradaColumn == null ? 0.0 : galloEntradaColumn.getWidth();
        fixed += galloOrdenColumn == null ? 0.0 : galloOrdenColumn.getWidth();
        fixed += (galloObligatorioColumn == null || !galloObligatorioColumn.isVisible()) ? 0.0 : galloObligatorioColumn.getWidth();
        double available = galloTable.getWidth() - fixed - reserved;
        galloAccionesColumn.setPrefWidth(Math.max(78.0, available));
    }

    private ObservableList<RondaOption> rondasDisponiblesPara(Gallo gallo) {
        ObservableList<RondaOption> opciones = FXCollections.observableArrayList();
        opciones.add(SIN_PREFERENCIA_RONDA);
        int maxRondas = eventoActual == null ? 0 : Math.max(0, eventoActual.getGallosPorPartido());
        for (int ronda = 1; ronda <= maxRondas; ronda++) {
            if (rondaDisponibleParaEntrada(gallo, ronda)) {
                opciones.add(optionForRonda(ronda));
            }
        }
        return opciones;
    }

    private boolean rondaDisponibleParaEntrada(Gallo gallo, int ronda) {
        if (gallo == null || ronda <= 0) {
            return true;
        }
        Integer rondaActual = gallo.getRondaPreferida();
        if (rondaActual != null && rondaActual == ronda) {
            return true;
        }
        int entrada = entradaIndexFor(gallo);
        if (entrada < 0) {
            return true;
        }
        for (Gallo otro : gallos) {
            if (otro == null || otro == gallo || sameGallo(otro, gallo)) {
                continue;
            }
            if (entradaIndexFor(otro) == entrada && otro.getRondaPreferida() != null && otro.getRondaPreferida() == ronda) {
                return false;
            }
        }
        return true;
    }

    private void aplicarRondaGallo(Gallo gallo, Integer ronda) {
        if (gallo == null) {
            return;
        }
        if (ronda != null && !rondaDisponibleParaEntrada(gallo, ronda)) {
            shellController.setStatus("Esa ronda ya esta asignada en la entrada " + (entradaIndexFor(gallo) + 1) + ".");
            galloTable.refresh();
            return;
        }
        try {
            gallo.setPreferenciaOrden(ronda == null ? PreferenciaOrdenGallo.SIN_PREFERENCIA : PreferenciaOrdenGallo.RONDA_ESPECIFICA);
            gallo.setRondaPreferida(ronda);
            galloRepository.actualizar(gallo);
            galloTable.refresh();
            shellController.setStatus(ronda == null ? "Ronda del gallo limpiada." : "Ronda del gallo actualizada.");
        } catch (Exception e) {
            shellController.showError("Error al actualizar ronda del gallo", e);
        }
    }

    private void aplicarObligatorioGallo(Gallo gallo, boolean obligatorio) {
        if (gallo == null) {
            return;
        }
        if (obligatorio && !obligatorioDisponibleParaEntrada(gallo)) {
            int max = maxObligatoriosPorEntrada();
            shellController.setStatus("Solo puedes marcar " + max + " gallo" + (max == 1 ? "" : "s") + " obligatorio" + (max == 1 ? "" : "s") + " por entrada.");
            galloTable.refresh();
            return;
        }
        try {
            gallo.setObligatorio(obligatorio);
            galloRepository.actualizar(gallo);
            galloTable.refresh();
        } catch (Exception e) {
            shellController.showError("Error al actualizar obligatorio", e);
        }
    }

    private boolean obligatorioDisponibleParaEntrada(Gallo gallo) {
        int max = maxObligatoriosPorEntrada();
        if (max <= 0) {
            return false;
        }
        int entrada = entradaIndexFor(gallo);
        if (entrada < 0) {
            return true;
        }
        int marcados = 0;
        for (Gallo otro : gallos) {
            if (otro == null || otro == gallo || sameGallo(otro, gallo)) {
                continue;
            }
            if (entradaIndexFor(otro) == entrada && otro.isObligatorio()) {
                marcados++;
            }
        }
        return marcados < max;
    }

    private int maxObligatoriosPorEntrada() {
        if (eventoActual == null) {
            return 0;
        }
        int gallosPorEntrada = Math.max(1, eventoActual.getGallosPorPartido());
        return Math.min(Math.max(0, eventoActual.getGallosObligatorios()), gallosPorEntrada);
    }

    private void eliminarGalloDirecto(Gallo gallo) {
        try {
            Partido selectedPartido = partidoTable.getSelectionModel().getSelectedItem();
            if (selectedPartido == null || gallo == null) {
                shellController.setStatus("Selecciona un gallo para eliminar.");
                return;
            }
            galloRepository.eliminar(gallo.getId());
            loadGallos(selectedPartido.getId(), null);
            clearGalloFields();
            shellController.setStatus("Gallo eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar gallo", e);
        }
    }

    private void eliminarPartidoDirecto(Partido partido) {
        try {
            if (partido == null) {
                shellController.setStatus("Selecciona un partido para eliminar.");
                return;
            }
            partidoRepository.eliminar(partido.getId());
            loadPartidos(null);
            gallos.clear();
            clearPartidoFields();
            clearGalloFields();
            if (shellController != null) {
                shellController.refrescarPartidosEnPeleas();
            }
            shellController.setStatus("Partido eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar partido", e);
        }
    }

    private int entradaIndexFor(Gallo gallo) {
        int index = indexOfGallo(gallo == null ? null : gallo.getId());
        if (index < 0) {
            return -1;
        }
        int gallosPorEntrada = eventoActual == null ? 0 : eventoActual.getGallosPorPartido();
        if (gallosPorEntrada <= 0) {
            return -1;
        }
        return index / gallosPorEntrada;
    }

    private boolean sameGallo(Gallo left, Gallo right) {
        if (left == null || right == null || left.getId() == null || right.getId() == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }

    private RondaOption optionForRonda(Integer ronda) {
        return ronda == null ? SIN_PREFERENCIA_RONDA : new RondaOption(ronda, "Ronda " + ronda);
    }

    private void configurarLayoutMitades() {
        if (registroSplitBox == null || partidosPanel == null || gallosPanel == null) {
            return;
        }
        Runnable ajustar = () -> {
            double spacing = registroSplitBox.getSpacing();
            double total = registroSplitBox.getWidth();
            if (total <= 0) {
                return;
            }
            double cadaPanel = Math.max(0.0, (total - spacing) / 2.0);
            partidosPanel.setMinWidth(0.0);
            partidosPanel.setPrefWidth(cadaPanel);
            partidosPanel.setMaxWidth(Double.MAX_VALUE);
            gallosPanel.setMinWidth(0.0);
            gallosPanel.setPrefWidth(cadaPanel);
            gallosPanel.setMaxWidth(Double.MAX_VALUE);
        };
        registroSplitBox.widthProperty().addListener((obs, oldWidth, newWidth) -> ajustar.run());
        Platform.runLater(ajustar);
    }

    private String formatearEntrada(Gallo gallo) {
        int gallosPorPartido = eventoActual == null ? 0 : eventoActual.getGallosPorPartido();
        return EntradaGallo.formatearEntrada(gallo, gallosPorPartido, partidoTieneMultiplesEntradas());
    }

    private boolean partidoTieneMultiplesEntradas() {
        int gallosPorPartido = eventoActual == null ? 0 : eventoActual.getGallosPorPartido();
        return gallosPorPartido > 0 && gallos.size() > gallosPorPartido;
    }

    private String formatNumero(double numero) {
        if (numero == Math.rint(numero)) {
            return String.valueOf((long) numero);
        }
        return String.valueOf(numero);
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private void configurePartidoTable() {
        if (partidoTable == null) {
            return;
        }

        partidoTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        partidoTable.setPlaceholder(new Label("No hay partidos registrados para el evento seleccionado."));

        if (partidoNombreColumn != null) {
            partidoNombreColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(safeText(cellData.getValue().getNombre())));
            partidoNombreColumn.setMinWidth(150.0);
        }
        if (partidoOrdenColumn != null) {
            partidoOrdenColumn.setMinWidth(150.0);
            partidoOrdenColumn.setPrefWidth(170.0);
            partidoOrdenColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            partidoOrdenColumn.setCellFactory(column -> new TableCell<Partido, Partido>() {
                private final ComboBox<PreferenciaOrdenPartido> ordenCombo =
                        new ComboBox<>(FXCollections.observableArrayList(PreferenciaOrdenPartido.values()));
                private boolean updating;

                {
                    ordenCombo.setMaxWidth(Double.MAX_VALUE);
                    ordenCombo.setOnAction(event -> {
                        if (updating) {
                            return;
                        }
                        Partido partido = getItem();
                        PreferenciaOrdenPartido preferencia = ordenCombo.getSelectionModel().getSelectedItem();
                        if (partido == null || preferencia == null || partido.getPreferenciaOrden() == preferencia) {
                            return;
                        }
                        aplicarPreferenciaOrdenPartido(partido, preferencia);
                    });
                }

                @Override
                protected void updateItem(Partido partido, boolean empty) {
                    super.updateItem(partido, empty);
                    if (empty || partido == null) {
                        setGraphic(null);
                        return;
                    }
                    updating = true;
                    ordenCombo.getSelectionModel().select(partido.getPreferenciaOrden());
                    updating = false;
                    setGraphic(ordenCombo);
                }
            });
        }
        if (partidoAccionesColumn != null) {
            partidoAccionesColumn.setMinWidth(130.0);
            partidoAccionesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            partidoAccionesColumn.setCellFactory(column -> new TableCell<Partido, Partido>() {
                private final Button eliminarButton = crearBotonEliminarIcono("Eliminar partido");

                {
                    eliminarButton.setOnAction(event -> eliminarPartidoDirecto(getItem()));
                }

                @Override
                protected void updateItem(Partido partido, boolean empty) {
                    super.updateItem(partido, empty);
                    setGraphic(empty || partido == null ? null : eliminarButton);
                }
            });
        }

        partidoTable.setRowFactory(table -> new TableRow<Partido>() {
            @Override
            protected void updateItem(Partido partido, boolean empty) {
                super.updateItem(partido, empty);
                getStyleClass().removeAll(PARTIDO_ROW_STYLES);
                if (empty || partido == null) {
                    return;
                }
                int index = getIndex();
                if (index >= 0) {
                    getStyleClass().add(PARTIDO_ROW_STYLES[index % PARTIDO_ROW_STYLES.length]);
                }
            }
        });
    }

    private void actualizarPartidoSeleccionadoLabel(Partido partido) {
        if (partidoSeleccionadoLabel == null) {
            return;
        }
        String nombre = partido == null ? "Partido no seleccionado" : safeText(partido.getNombre());
        partidoSeleccionadoLabel.setText(nombre);
    }

    private void aplicarPreferenciaOrdenPartido(Partido partido, PreferenciaOrdenPartido preferencia) {
        if (partido == null || preferencia == null || eventoActual == null) {
            return;
        }
        try {
            partido.setPreferenciaOrden(preferencia);
            partidoRepository.actualizar(partido);
            partidoTable.refresh();
            if (shellController != null) {
                shellController.refrescarPartidosEnPeleas();
            }
            shellController.setStatus("Orden del partido actualizado.");
        } catch (Exception e) {
            shellController.showError("Error al actualizar orden del partido", e);
            partidoTable.refresh();
        }
    }

    private void actualizarVisibilidadColumnaObligatorio() {
        if (galloObligatorioColumn == null) {
            return;
        }
        boolean visible = eventoActual != null && eventoActual.getGallosObligatorios() > 0;
        galloObligatorioColumn.setVisible(visible);
        if (galloTable != null) {
            galloTable.refresh();
        }
    }

    private void actualizarVisibilidadColumnasGallos() {
        if (galloOrdenColumn != null) {
            boolean visibleRonda = eventoActual != null && eventoActual.getGallosPorPartido() > 1;
            galloOrdenColumn.setVisible(visibleRonda);
        }
        actualizarVisibilidadColumnaObligatorio();
    }

    private Button crearBotonEliminarIcono(String tooltip) {
        SVGPath icono = new SVGPath();
        icono.setContent("M6 7h12l-1 14H7L6 7zm3-3h6l1 2H8l1-2zm1 5v8h2V9h-2zm4 0v8h2V9h-2z");
        icono.setFill(Color.WHITE);
        icono.setScaleX(0.72);
        icono.setScaleY(0.72);

        Button boton = new Button();
        boton.getStyleClass().add("danger-action");
        boton.getStyleClass().add("fight-action-button");
        boton.setGraphic(icono);
        boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        boton.setPrefWidth(34.0);
        boton.setMinWidth(34.0);
        boton.setMaxWidth(34.0);
        boton.setStyle("-fx-padding: 0;");
        boton.setTooltip(new Tooltip(tooltip));
        return boton;
    }

    private static final class RondaOption {
        private final Integer ronda;
        private final String etiqueta;

        private RondaOption(Integer ronda, String etiqueta) {
            this.ronda = ronda;
            this.etiqueta = etiqueta;
        }

        private Integer getRonda() {
            return ronda;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof RondaOption)) {
                return false;
            }
            RondaOption other = (RondaOption) object;
            return java.util.Objects.equals(ronda, other.ronda);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(ronda);
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
