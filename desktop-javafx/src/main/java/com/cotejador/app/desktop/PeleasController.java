package com.cotejador.app.desktop;

import com.cotejador.app.core.cotejo.MotorCotejo;
import com.cotejador.app.core.cotejo.OrdenadorPeleas;
import com.cotejador.app.core.cotejo.ParametrosCotejo;
import com.cotejador.app.core.cotejo.ParametrosOrdenamiento;
import com.cotejador.app.core.cotejo.Pelea;
import com.cotejador.app.core.cotejo.ResultadoCotejo;
import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.EntradaGallo;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.ModoCotejo;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PeleaGuardada;
import com.cotejador.app.core.model.PreferenciaOrdenGallo;
import com.cotejador.app.core.model.PreferenciaOrdenPartido;
import com.cotejador.app.core.model.RestriccionPartido;
import com.cotejador.app.data.sqlite.CotejoRepository;
import com.cotejador.app.data.sqlite.EventoRepository;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import com.cotejador.app.data.sqlite.PdfCotejoService;
import com.cotejador.app.data.sqlite.RestriccionPartidoRepository;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.css.PseudoClass;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeleasController {
    private static final DateTimeFormatter COTEJO_FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PseudoClass DROP_TOP = PseudoClass.getPseudoClass("drop-top");
    private static final PseudoClass DROP_BOTTOM = PseudoClass.getPseudoClass("drop-bottom");
    private static final PseudoClass DRAG_ACTIVE = PseudoClass.getPseudoClass("drag-active");
    private static final double DEFAULT_TOLERANCIA = 80.0;
    private static final double MIN_TOLERANCIA = 0.0;
    private static final double MAX_TOLERANCIA = 10000.0;
    private static final double TOLERANCIA_STEP = 1.0;
    private static final double PARTIDOS_MIN_WIDTH = 120.0;
    private static final double GALLOS_SIN_PELEA_PESO_WIDTH = 88.0;
    private static final double GALLOS_SIN_PELEA_ANILLO_WIDTH = 92.0;
    private static final double GALLOS_SIN_PELEA_RONDA_WIDTH = 86.0;
    private static final double GALLOS_SIN_PELEA_PARTIDO_MIN_WIDTH = 120.0;
    private double lastToleranciaUsada = DEFAULT_TOLERANCIA;
    private ShellController shellController;
    private EventoRepository eventoRepository;
    private PartidoRepository partidoRepository;
    private GalloRepository galloRepository;
    private CotejoRepository cotejoRepository;
    private PdfCotejoService pdfCotejoService;
    private RestriccionPartidoRepository restriccionPartidoRepository;
    private final MotorCotejo motorCotejo = new MotorCotejo();
    private final OrdenadorPeleas ordenadorPeleas = new OrdenadorPeleas();

    private Evento eventoActual;

    private final ObservableList<Partido> partidos = FXCollections.observableArrayList();
    private final ObservableList<Pelea> peleasCotejo = FXCollections.observableArrayList();
    private final ObservableList<PeleaFila> peleasCotejoFilas = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallosSinPelea = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallosEvento = FXCollections.observableArrayList();
    private final ObservableList<RestriccionPartido> restricciones = FXCollections.observableArrayList();
    private final ObservableList<CotejoGuardado> cotejosGuardados = FXCollections.observableArrayList();
    private final ObjectProperty<Pelea> peleaSeleccionada = new SimpleObjectProperty<>();
    private final Map<Long, String> nombresPartidosEvento = new HashMap<>();
    private final Map<Long, Integer> rondaPorGalloId = new HashMap<>();
    private CotejoGuardado cotejoEditableActual;
    private CotejoGuardado cotejoImpresionActual;

    @FXML
    private Spinner<Double> toleranciaSpinner;
    @FXML
    private RadioButton modoAleatorioRadio;
    @FXML
    private RadioButton modoRondasRadio;
    @FXML
    private ToggleGroup modoCotejoToggleGroup;
    @FXML
    private CheckBox ultimaRondaSoloObligatoriosCheckBox;
    @FXML
    private CheckBox excluirObligatoriosDelCotejoCheckBox;
    @FXML
    private VBox reglasCotejoBox;
    @FXML
    private TitledPane reglasCotejoPane;
    @FXML
    private SplitPane peleaSplitPane;
    @FXML
    private TableView<PeleaFila> peleasCotejoTable;
    @FXML
    private TableColumn<PeleaFila, Number> peleaNumeroColumn;
    @FXML
    private TableColumn<PeleaFila, Number> peleaRondaColumn;
    @FXML
    private TableColumn<PeleaFila, String> peleaGallo1Column;
    @FXML
    private TableColumn<PeleaFila, String> peleaPeso1Column;
    @FXML
    private TableColumn<PeleaFila, String> peleaGallo2Column;
    @FXML
    private TableColumn<PeleaFila, String> peleaPeso2Column;
    @FXML
    private TableColumn<PeleaFila, String> peleaDiferenciaColumn;
    @FXML
    private TableColumn<PeleaFila, String> peleaPartidosColumn;
    @FXML
    private TableColumn<PeleaFila, Void> peleaEditarColumn;
    @FXML
    private VBox peleaDetallePane;
    @FXML
    private Label peleaDetalleTitulo;
    @FXML
    private Label peleaDetalleResumenLabel;
    @FXML
    private Label peleaDetalleDiferenciaLabel;
    @FXML
    private Label peleaDetallePartido1Label;
    @FXML
    private Label peleaDetallePartido2Label;
    @FXML
    private Label peleaDetalleGallo1Label;
    @FXML
    private Label peleaDetallePeso1Label;
    @FXML
    private Label peleaDetalleAnillo1Label;
    @FXML
    private Label peleaDetallePartido1TextoLabel;
    @FXML
    private Label peleaDetalleGallo2Label;
    @FXML
    private Label peleaDetallePeso2Label;
    @FXML
    private Label peleaDetalleAnillo2Label;
    @FXML
    private Label peleaDetallePartido2TextoLabel;
    @FXML
    private Label peleaDetalleDifPesoLabel;
    @FXML
    private TextField peleaDetalleGallo1PesoField;
    @FXML
    private TextField peleaDetalleGallo1AnilloField;
    @FXML
    private TextField peleaDetallePartido1Field;
    @FXML
    private ComboBox<Gallo> peleaDetalleGallo1Combo;
    @FXML
    private TextField peleaDetalleGallo2PesoField;
    @FXML
    private TextField peleaDetalleGallo2AnilloField;
    @FXML
    private TextField peleaDetallePartido2Field;
    @FXML
    private ComboBox<Gallo> peleaDetalleGallo2Combo;
    @FXML
    private TableView<Gallo> gallosSinPeleaTable;
    @FXML
    private TableColumn<Gallo, String> gallosSinPeleaPartidoColumn;
    @FXML
    private TableColumn<Gallo, String> gallosSinPeleaPesoColumn;
    @FXML
    private TableColumn<Gallo, String> gallosSinPeleaAnilloColumn;
    @FXML
    private TableColumn<Gallo, String> gallosSinPeleaRondaColumn;
    @FXML
    private TitledPane gallosSinPeleaPane;
    @FXML
    private TitledPane restriccionesPane;
    @FXML
    private TitledPane cotejosGuardadosPane;
    @FXML
    private ComboBox<Partido> restriccionPartidoOrigenCombo;
    @FXML
    private ComboBox<Partido> restriccionPartidoDestinoCombo;
    @FXML
    private ListView<RestriccionPartido> restriccionList;
    @FXML
    private TableView<CotejoGuardado> cotejoGuardadoTable;
    @FXML
    private TableColumn<CotejoGuardado, Number> cotejoIdColumn;
    @FXML
    private TableColumn<CotejoGuardado, String> cotejoFechaColumn;
    @FXML
    private TableColumn<CotejoGuardado, String> cotejoToleranciaColumn;
    @FXML
    private Button guardarCotejoButton;
    @FXML
    private Button guardarImprimirButton;
    @FXML
    private HBox opcionesImpresionPane;
    @FXML
    private Label opcionesImpresionTituloLabel;
    @FXML
    private Button peleaAplicarButton;
    @FXML
    private Button peleaEliminarButton;

    public void setShellController(ShellController shellController) {
        this.shellController = shellController;
        aplicarTemaDetallePelea();
    }

    public void setRepositories(EventoRepository eventoRepository,
                           PartidoRepository partidoRepository,
                           GalloRepository galloRepository,
                           CotejoRepository cotejoRepository,
                           PdfCotejoService pdfCotejoService,
                           RestriccionPartidoRepository restriccionPartidoRepository) {
        this.eventoRepository = eventoRepository;
        this.partidoRepository = partidoRepository;
        this.galloRepository = galloRepository;
        this.cotejoRepository = cotejoRepository;
        this.pdfCotejoService = pdfCotejoService;
        this.restriccionPartidoRepository = restriccionPartidoRepository;

        initializeComboBoxes();
        initializeReglasCotejo();
        initializeToleranciaSpinner();
        initializePeleasTable();
        initializeDetallePelea();
        bindListViews();
        configureCotejosGuardadosList();
        actualizarExpansionGallosSinPelea();
        aplicarProporcionSplitPane();
    }

    private void bindListViews() {
        peleasCotejo.addListener((ListChangeListener<Pelea>) change -> rebuildPeleasCotejoFilas());
        peleasCotejoTable.setItems(peleasCotejoFilas);
        rebuildPeleasCotejoFilas();
        if (gallosSinPeleaTable != null) {
            gallosSinPeleaTable.setItems(gallosSinPelea);
            gallosSinPeleaTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }
        if (gallosSinPeleaPartidoColumn != null) {
            gallosSinPeleaPartidoColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(nombrePartidoConEntrada(cellData.getValue())));
        }
        if (gallosSinPeleaPesoColumn != null) {
            gallosSinPeleaPesoColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearPeso(cellData.getValue().getPeso())));
        }
        if (gallosSinPeleaAnilloColumn != null) {
            gallosSinPeleaAnilloColumn.setCellValueFactory(cellData -> {
                String anillo = cellData.getValue().getAnillo();
                return new ReadOnlyStringWrapper(anillo == null || anillo.trim().isEmpty() ? "-" : anillo.trim());
            });
        }
        if (gallosSinPeleaRondaColumn != null) {
            gallosSinPeleaRondaColumn.setCellValueFactory(cellData -> {
                Long galloId = cellData.getValue().getId();
                Integer ronda = galloId == null ? null : rondaPorGalloId.get(galloId);
                return new ReadOnlyStringWrapper(ronda == null ? "-" : String.valueOf(ronda));
            });
        }
        configurarTablaGallosSinPelea();
        restriccionList.setItems(restricciones);
        if (cotejoGuardadoTable != null) {
            cotejoGuardadoTable.setItems(cotejosGuardados);
            cotejoGuardadoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (cotejoIdColumn != null) {
            cotejoIdColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(cellData.getValue() == null ? null : cellData.getValue().getId()));
        }
        if (cotejoFechaColumn != null) {
            cotejoFechaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(cellData.getValue() == null
                            ? ""
                            : formatearFechaCotejo(cellData.getValue().getFechaGeneracion())));
        }
        if (cotejoToleranciaColumn != null) {
            cotejoToleranciaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(cellData.getValue() == null
                            ? ""
                            : formatearPeso(cellData.getValue().getToleranciaGramos()) + " g"));
        }
    }

    private void configurarTablaGallosSinPelea() {
        if (gallosSinPeleaTable == null
                || gallosSinPeleaPartidoColumn == null
                || gallosSinPeleaPesoColumn == null
                || gallosSinPeleaAnilloColumn == null
                || gallosSinPeleaRondaColumn == null) {
            return;
        }

        gallosSinPeleaPesoColumn.setResizable(false);
        gallosSinPeleaAnilloColumn.setResizable(false);
        gallosSinPeleaRondaColumn.setResizable(false);
        gallosSinPeleaPartidoColumn.setResizable(true);

        gallosSinPeleaPesoColumn.setMinWidth(GALLOS_SIN_PELEA_PESO_WIDTH);
        gallosSinPeleaPesoColumn.setPrefWidth(GALLOS_SIN_PELEA_PESO_WIDTH);
        gallosSinPeleaPesoColumn.setMaxWidth(GALLOS_SIN_PELEA_PESO_WIDTH);

        gallosSinPeleaAnilloColumn.setMinWidth(GALLOS_SIN_PELEA_ANILLO_WIDTH);
        gallosSinPeleaAnilloColumn.setPrefWidth(GALLOS_SIN_PELEA_ANILLO_WIDTH);
        gallosSinPeleaAnilloColumn.setMaxWidth(GALLOS_SIN_PELEA_ANILLO_WIDTH);

        gallosSinPeleaRondaColumn.setMinWidth(GALLOS_SIN_PELEA_RONDA_WIDTH);
        gallosSinPeleaRondaColumn.setPrefWidth(GALLOS_SIN_PELEA_RONDA_WIDTH);
        gallosSinPeleaRondaColumn.setMaxWidth(GALLOS_SIN_PELEA_RONDA_WIDTH);

        gallosSinPeleaPartidoColumn.setMinWidth(GALLOS_SIN_PELEA_PARTIDO_MIN_WIDTH);

        Runnable ajustar = () -> {
            double reserved = 24.0;
            double fixedWidth = GALLOS_SIN_PELEA_PESO_WIDTH + GALLOS_SIN_PELEA_ANILLO_WIDTH + GALLOS_SIN_PELEA_RONDA_WIDTH;
            double available = gallosSinPeleaTable.getWidth() - fixedWidth - reserved;
            gallosSinPeleaPartidoColumn.setPrefWidth(Math.max(GALLOS_SIN_PELEA_PARTIDO_MIN_WIDTH, available));
        };

        gallosSinPeleaTable.widthProperty().addListener((obs, oldWidth, newWidth) -> ajustar.run());
        Platform.runLater(ajustar);
    }

    private void actualizarExpansionGallosSinPelea() {
        if (gallosSinPeleaPane != null) {
            gallosSinPeleaPane.setExpanded(eventoActual != null && eventoActual.getGallosPorPartido() == 1);
        }
    }

    private void initializeComboBoxes() {
        restriccionPartidoOrigenCombo.setItems(partidos);
        restriccionPartidoDestinoCombo.setItems(partidos);
    }

    private void initializeReglasCotejo() {
        if (modoCotejoToggleGroup != null) {
            if (modoAleatorioRadio != null) {
                modoAleatorioRadio.setSelected(true);
            }
            modoCotejoToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> notifyRuleChange());
        }
        if (excluirObligatoriosDelCotejoCheckBox != null && ultimaRondaSoloObligatoriosCheckBox != null) {
            excluirObligatoriosDelCotejoCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                if (selected) {
                    ultimaRondaSoloObligatoriosCheckBox.setSelected(false);
                }
                ultimaRondaSoloObligatoriosCheckBox.setDisable(selected);
                notifyRuleChange();
            });
        }
        if (ultimaRondaSoloObligatoriosCheckBox != null) {
            ultimaRondaSoloObligatoriosCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> notifyRuleChange());
        }
    }

    private void aplicarProporcionSplitPane() {
        if (peleaSplitPane != null) {
            Runnable aplicar = () -> peleaSplitPane.setDividerPositions(0.25);
            if (peleaSplitPane.getScene() != null) {
                Platform.runLater(aplicar);
            } else {
                peleaSplitPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
                    if (newScene != null) {
                        Platform.runLater(aplicar);
                    }
                });
            }
            peleaSplitPane.widthProperty().addListener((observable, oldWidth, newWidth) -> Platform.runLater(aplicar));
        }
    }

    private void initializePeleasTable() {
        if (peleasCotejoTable == null) {
            return;
        }

        peleasCotejoTable.setPlaceholder(new Label("No hay peleas generadas todavía."));
        peleasCotejoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        peleasCotejoTable.setSortPolicy(table -> false);

        if (peleaNumeroColumn != null) {
            peleaNumeroColumn.setResizable(false);
            peleaNumeroColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(peleasCotejoFilas.indexOf(cellData.getValue()) + 1));
        }
        if (peleaRondaColumn != null) {
            peleaRondaColumn.setResizable(false);
            peleaRondaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(cellData.getValue().getNumeroRondaMostrada()));
        }
        if (peleaGallo1Column != null) {
            peleaGallo1Column.setResizable(false);
            peleaGallo1Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearAnilloCompacto(cellData.getValue().getPelea().getGallo1())));
        }
        if (peleaPeso1Column != null) {
            peleaPeso1Column.setResizable(false);
            peleaPeso1Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearPesoCompacto(cellData.getValue().getPelea().getGallo1().getPeso())));
        }
        if (peleaGallo2Column != null) {
            peleaGallo2Column.setResizable(false);
            peleaGallo2Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearAnilloCompacto(cellData.getValue().getPelea().getGallo2())));
        }
        if (peleaPeso2Column != null) {
            peleaPeso2Column.setResizable(false);
            peleaPeso2Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearPesoCompacto(cellData.getValue().getPelea().getGallo2().getPeso())));
        }
        if (peleaDiferenciaColumn != null) {
            peleaDiferenciaColumn.setResizable(false);
            peleaDiferenciaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearDiferenciaCompacta(cellData.getValue().getPelea().getDiferenciaPeso())));
        }
        if (peleaPartidosColumn != null) {
            peleaPartidosColumn.setResizable(true);
            peleaPartidosColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(
                            nombrePartidoConEntrada(cellData.getValue().getPelea().getGallo1()) +
                                    " / " +
                                    nombrePartidoConEntrada(cellData.getValue().getPelea().getGallo2())));
        }
        if (peleaEditarColumn != null) {
            peleaEditarColumn.setSortable(false);
            peleaEditarColumn.setResizable(false);
            peleaEditarColumn.setCellFactory(column -> new TableCell<PeleaFila, Void>() {
                private final Button editarButton = crearBotonEditarPelea();
                private final Button eliminarButton = crearBotonEliminarPelea();
                private final HBox accionesBox = new HBox(6, editarButton, eliminarButton);

                {
                    editarButton.setOnAction(event -> {
                        PeleaFila fila = getTableView().getItems().get(getIndex());
                        abrirDetallePelea(fila == null ? null : fila.getPelea());
                    });
                    eliminarButton.setOnAction(event -> eliminarPeleaEnIndice(getIndex()));
                    setAlignment(Pos.CENTER);
                    accionesBox.setAlignment(Pos.CENTER);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : accionesBox);
                }
            });
        }

        configurarColumnaPartidosResponsiva();
        peleasCotejoTable.setRowFactory(table -> crearFilaArrastrable());
    }

    private void initializeDetallePelea() {
        configurarComboGallo(peleaDetalleGallo1Combo);
        configurarComboGallo(peleaDetalleGallo2Combo);
        if (peleaDetalleGallo1Combo != null) {
            peleaDetalleGallo1Combo.valueProperty().addListener((observable, oldValue, newValue) ->
                    cargarCamposGalloDetalle(1, newValue));
        }
        if (peleaDetalleGallo2Combo != null) {
            peleaDetalleGallo2Combo.valueProperty().addListener((observable, oldValue, newValue) ->
                    cargarCamposGalloDetalle(2, newValue));
        }
        aplicarTemaDetallePelea();
        ocultarDetallePelea();
    }

    private void configurarColumnaPartidosResponsiva() {
        if (peleasCotejoTable == null || peleaPartidosColumn == null) {
            return;
        }

        Runnable ajustar = () -> {
            double fixedWidth = 0.0;
            fixedWidth += anchoColumna(peleaNumeroColumn);
            fixedWidth += anchoColumna(peleaRondaColumn);
            fixedWidth += anchoColumna(peleaGallo1Column);
            fixedWidth += anchoColumna(peleaPeso1Column);
            fixedWidth += anchoColumna(peleaGallo2Column);
            fixedWidth += anchoColumna(peleaPeso2Column);
            fixedWidth += anchoColumna(peleaDiferenciaColumn);
            fixedWidth += anchoColumna(peleaEditarColumn);

            double reserved = 26.0;
            double available = peleasCotejoTable.getWidth() - fixedWidth - reserved;
            peleaPartidosColumn.setPrefWidth(Math.max(PARTIDOS_MIN_WIDTH, available));
        };

        peleasCotejoTable.widthProperty().addListener((obs, oldWidth, newWidth) -> ajustar.run());
        Platform.runLater(ajustar);
    }

    private double anchoColumna(TableColumn<?, ?> column) {
        return column == null ? 0.0 : Math.max(0.0, column.getWidth());
    }

    private void configureCotejosGuardadosList() {
        if (cotejoGuardadoTable == null) {
            return;
        }
        cotejoGuardadoTable.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() < 2) {
                return;
            }
            cargarCotejoSeleccionadoEnTablaPrincipal();
        });
    }

    public void onEventoCambio(Evento evento) {
        if (evento == null) {
            limpiar();
            return;
        }

        this.eventoActual = evento;
        clearResultadosVisuales();
        setDefaultRulesFromEvento(evento);

        try {
            PartidoRepository partidoRepo = getPartidoRepository();
            if (partidoRepo != null) {
                List<Partido> partidosDelEvento = partidoRepo.listarPorEvento(evento.getId());
                partidos.setAll(partidosDelEvento);
                gallosEvento.setAll(loadGallosDelEvento(evento));
                calcularRondasGallosEvento();
                cargarNombresPartidos(evento);
                loadRestricciones(evento.getId());
                loadCotejosGuardados(evento.getId());
            }
        } catch (Exception e) {
            shellController.setStatus("Error al cargar datos del evento: " + e.getMessage());
        }
    }

    public void limpiar() {
        eventoActual = null;
        clearResultadosVisuales();
    }

    private void clearResultadosVisuales() {
        partidos.clear();
        peleasCotejo.clear();
        gallosSinPelea.clear();
        restricciones.clear();
        cotejosGuardados.clear();
        gallosEvento.clear();
        nombresPartidosEvento.clear();
        rondaPorGalloId.clear();
        cotejoEditableActual = null;
        cotejoImpresionActual = null;
        peleaSeleccionada.set(null);
        if (reglasCotejoPane != null) {
            reglasCotejoPane.setVisible(false);
            reglasCotejoPane.setManaged(false);
            reglasCotejoPane.setExpanded(false);
        }
        if (reglasCotejoBox != null) {
            reglasCotejoBox.setVisible(false);
            reglasCotejoBox.setManaged(false);
        }
        if (ultimaRondaSoloObligatoriosCheckBox != null) {
            ultimaRondaSoloObligatoriosCheckBox.setSelected(false);
            ultimaRondaSoloObligatoriosCheckBox.setDisable(false);
        }
        if (modoAleatorioRadio != null) {
            modoAleatorioRadio.setSelected(true);
        }
        if (excluirObligatoriosDelCotejoCheckBox != null) {
            excluirObligatoriosDelCotejoCheckBox.setSelected(false);
        }
        if (restriccionPartidoOrigenCombo != null) {
            restriccionPartidoOrigenCombo.getSelectionModel().clearSelection();
        }
        if (restriccionPartidoDestinoCombo != null) {
            restriccionPartidoDestinoCombo.getSelectionModel().clearSelection();
        }
        if (cotejoGuardadoTable != null) {
            cotejoGuardadoTable.getSelectionModel().clearSelection();
        }
        actualizarExpansionGallosSinPelea();
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().clearSelection();
        }
        ocultarDetallePelea();
        cerrarPanelOpcionesImpresion();
    }

    private PartidoRepository getPartidoRepository() {
        return partidoRepository;
    }

    private void initializeToleranciaSpinner() {
        if (toleranciaSpinner == null) {
            return;
        }

        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        MIN_TOLERANCIA,
                        MAX_TOLERANCIA,
                        lastToleranciaUsada,
                        TOLERANCIA_STEP);
        toleranciaSpinner.setValueFactory(valueFactory);
        toleranciaSpinner.setEditable(false);
        toleranciaSpinner.getValueFactory().setValue(lastToleranciaUsada);
        toleranciaSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                lastToleranciaUsada = newValue;
            }
        });
    }

    private Double readTolerancia() {
        if (toleranciaSpinner == null || toleranciaSpinner.getValue() == null) {
            return null;
        }
        return toleranciaSpinner.getValue();
    }

    private void cargarNombresPartidos(Evento evento) throws Exception {
        nombresPartidosEvento.clear();
        if (evento == null) {
            return;
        }
        List<Partido> partidosDelEvento = partidoRepository.listarPorEvento(evento.getId());
        for (Partido partido : partidosDelEvento) {
            nombresPartidosEvento.put(partido.getId(), partido.getNombre());
        }
    }

    private void loadRestricciones(Long eventoId) throws Exception {
        restricciones.setAll(restriccionPartidoRepository.listarPorEvento(eventoId));
    }

    private void loadCotejosGuardados(Long eventoId) throws Exception {
        cotejosGuardados.setAll(cotejoRepository.listarPorEvento(eventoId));
    }

    @FXML
    private void cotejarEvento() {
        if (eventoActual == null) {
            shellController.setStatus("Selecciona un evento para cotejar.");
            return;
        }

        try {
            cargarNombresPartidos(eventoActual);
            Map<Long, String> nombresPartidos = new HashMap<>(nombresPartidosEvento);
            List<RestriccionPartido> restriccionesPartidos =
                    restriccionPartidoRepository.listarPorEvento(eventoActual.getId());
            gallosEvento.setAll(loadGallosDelEvento(eventoActual));
            String errorEntradas = validarEntradasAntesDeCotejar(eventoActual, partidos, gallosEvento);
            if (errorEntradas != null) {
                shellController.setStatus(errorEntradas);
                return;
            }
            ParametrosCotejo parametros = readParametrosCotejo(nombresPartidos, restriccionesPartidos);
            if (parametros == null) {
                return;
            }

            List<Gallo> gallosDelEvento = new ArrayList<>(gallosEvento);
            ResultadoCotejo resultado = motorCotejo.cotejar(gallosDelEvento, parametros);
            rondaPorGalloId.clear();
            rondaPorGalloId.putAll(resultado.getRondasPorGalloId());
            List<Pelea> peleasOrdenadas = ordenadorPeleas.ordenar(
                    resultado.getPeleas(),
                    buildParametrosOrdenamiento(partidos, gallosDelEvento)
            );
            peleasCotejo.setAll(peleasOrdenadas);
            gallosSinPelea.setAll(resultado.getGallosSinPelea());

            cotejoEditableActual = null;
            if (peleasCotejoTable != null) {
                peleasCotejoTable.getSelectionModel().clearSelection();
            }
            ocultarDetallePelea();

            shellController.setStatus("Cotejo generado: " + resultado.getPeleas().size() +
                    " peleas, " + resultado.getGallosSinPelea().size() + " gallos sin pelea.");
        } catch (Exception e) {
            shellController.showError("Error al cotejar evento", e);
        }
    }

    @FXML
    private void guardarCotejoActual() {
        if (eventoActual == null) {
            shellController.setStatus("Selecciona un evento antes de guardar.");
            return;
        }
        if (peleasCotejo.isEmpty() && gallosSinPelea.isEmpty()) {
            shellController.setStatus("No hay cotejo generado para guardar.");
            return;
        }

        try {
            guardarOCrearCotejoActual();
            shellController.setStatus("Cotejo guardado.");
        } catch (Exception e) {
            shellController.showError("Error al guardar cotejo", e);
        }
    }

    @FXML
    private void guardarEImprimirDesdePeleas() {
        if (eventoActual == null) {
            shellController.setStatus("Selecciona un evento antes de guardar e imprimir.");
            return;
        }
        if (peleasCotejo.isEmpty() && gallosSinPelea.isEmpty()) {
            shellController.setStatus("No hay cotejo generado para guardar e imprimir.");
            return;
        }

        try {
            CotejoGuardado cotejo = guardarOCrearCotejoActual();
            mostrarPanelOpcionesImpresion(cotejo);
            shellController.setStatus("Cotejo guardado. Selecciona una opcion de impresion.");
        } catch (Exception e) {
            shellController.showError("Error al guardar e imprimir", e);
        }
    }

    private List<Gallo> loadGallosDelEvento(Evento evento) throws Exception {
        List<Gallo> gallosDelEvento = new ArrayList<>();
        List<Partido> partidosDelEvento = partidoRepository.listarPorEvento(evento.getId());
        for (Partido partido : partidosDelEvento) {
            gallosDelEvento.addAll(galloRepository.listarPorPartido(partido.getId()));
        }
        return gallosDelEvento;
    }

    private String validarEntradasAntesDeCotejar(Evento evento, List<Partido> partidosEvento, List<Gallo> gallosDelEvento) {
        if (evento == null || partidosEvento == null || gallosDelEvento == null) {
            return null;
        }
        int gallosPorEntrada = Math.max(1, evento.getGallosPorPartido());
        int obligatoriosPorEntrada = Math.min(Math.max(0, evento.getGallosObligatorios()), gallosPorEntrada);
        Map<Long, List<Gallo>> gallosPorPartido = agruparGallosPorPartido(gallosDelEvento);

        for (Partido partido : partidosEvento) {
            if (partido == null || partido.getId() == null) {
                continue;
            }
            List<Gallo> gallosPartido = gallosPorPartido.get(partido.getId());
            if (gallosPartido == null || gallosPartido.isEmpty()) {
                continue;
            }

            int sobrantes = gallosPartido.size() % gallosPorEntrada;
            if (sobrantes != 0) {
                int entrada = (gallosPartido.size() / gallosPorEntrada) + 1;
                int faltantes = gallosPorEntrada - sobrantes;
                return "No se puede cotejar: " + nombrePartidoSeguro(partido) +
                        ", entrada " + entrada + ", necesita " + faltantes +
                        " gallo" + (faltantes == 1 ? "" : "s") + " mas.";
            }

            if (obligatoriosPorEntrada > 0) {
                String errorObligatorios = validarObligatoriosPorEntrada(
                        partido,
                        gallosPartido,
                        gallosPorEntrada,
                        obligatoriosPorEntrada);
                if (errorObligatorios != null) {
                    return errorObligatorios;
                }
            }
        }
        return null;
    }

    private Map<Long, List<Gallo>> agruparGallosPorPartido(List<Gallo> gallosDelEvento) {
        Map<Long, List<Gallo>> gallosPorPartido = new HashMap<>();
        for (Gallo gallo : gallosDelEvento) {
            if (gallo == null || gallo.getPartidoId() == null) {
                continue;
            }
            if (!gallosPorPartido.containsKey(gallo.getPartidoId())) {
                gallosPorPartido.put(gallo.getPartidoId(), new ArrayList<>());
            }
            gallosPorPartido.get(gallo.getPartidoId()).add(gallo);
        }
        return gallosPorPartido;
    }

    private String validarObligatoriosPorEntrada(Partido partido,
                                                 List<Gallo> gallosPartido,
                                                 int gallosPorEntrada,
                                                 int obligatoriosPorEntrada) {
        int entradas = gallosPartido.size() / gallosPorEntrada;
        for (int entrada = 0; entrada < entradas; entrada++) {
            int inicio = entrada * gallosPorEntrada;
            int fin = inicio + gallosPorEntrada;
            int marcados = 0;
            for (int i = inicio; i < fin; i++) {
                if (gallosPartido.get(i).isObligatorio()) {
                    marcados++;
                }
            }
            if (marcados < obligatoriosPorEntrada) {
                int faltantes = obligatoriosPorEntrada - marcados;
                return "No se puede cotejar: " + nombrePartidoSeguro(partido) +
                        ", entrada " + (entrada + 1) + ", necesita " + faltantes +
                        " gallo" + (faltantes == 1 ? "" : "s") + " obligatorio" +
                        (faltantes == 1 ? "" : "s") + ".";
            }
        }
        return null;
    }

    private String nombrePartidoSeguro(Partido partido) {
        if (partido == null || partido.getNombre() == null || partido.getNombre().trim().isEmpty()) {
            return "Partido sin nombre";
        }
        return partido.getNombre().trim();
    }

    private ParametrosOrdenamiento buildParametrosOrdenamiento(List<Partido> partidosEvento, List<Gallo> gallosDelEvento) {
        Map<Long, PreferenciaOrdenPartido> preferenciasPartidos = new HashMap<>();
        Map<Long, PreferenciaOrdenGallo> preferenciasGallos = new HashMap<>();
        Map<Long, Integer> rondasPreferidasGallos = new HashMap<>();

        if (partidosEvento != null) {
            for (Partido partido : partidosEvento) {
                if (partido != null && partido.getId() != null
                        && partido.getPreferenciaOrden() != PreferenciaOrdenPartido.NORMAL) {
                    preferenciasPartidos.put(partido.getId(), partido.getPreferenciaOrden());
                }
            }
        }
        if (gallosDelEvento != null) {
            for (Gallo gallo : gallosDelEvento) {
                if (gallo == null || gallo.getId() == null
                        || gallo.getPreferenciaOrden() == PreferenciaOrdenGallo.SIN_PREFERENCIA) {
                    continue;
                }
                preferenciasGallos.put(gallo.getId(), gallo.getPreferenciaOrden());
                if (gallo.getRondaPreferida() != null) {
                    rondasPreferidasGallos.put(gallo.getId(), gallo.getRondaPreferida());
                }
            }
        }

        return new ParametrosOrdenamiento(preferenciasPartidos, preferenciasGallos, rondasPreferidasGallos);
    }

    private ParametrosCotejo readParametrosCotejo(Map<Long, String> nombresPartidos,
                                          List<RestriccionPartido> restriccionesPartidos) {
        Double tolerancia = readTolerancia();
        if (tolerancia == null) {
            shellController.setStatus("La tolerancia es obligatoria.");
            return null;
        }

        if (tolerancia < 0) {
            shellController.setStatus("La tolerancia no puede ser negativa.");
            return null;
        }

        boolean ultimaRondaSoloObligatorios = isUltimaRondaSoloObligatorios();

        return new ParametrosCotejo(
                tolerancia,
                nombresPartidos,
                restriccionesPartidos,
                eventoActual.getGallosPorPartido(),
                eventoActual.getGallosObligatorios(),
                ultimaRondaSoloObligatorios,
                readModoCotejo(),
                isExcluirObligatoriosDelCotejo());
    }

    private double toleranciaFromParametros(ParametrosCotejo parametros) {
        return parametros.getToleranciaGramos();
    }

    private void setDefaultRulesFromEvento(Evento evento) {
        if (evento == null) {
            return;
        }
        if (modoAleatorioRadio != null && modoRondasRadio != null) {
            boolean porRondas = evento.getGallosPorPartido() > 1 || evento.getModoCotejo() == ModoCotejo.RONDAS;
            if (porRondas) {
                modoRondasRadio.setSelected(true);
            } else {
                modoAleatorioRadio.setSelected(true);
            }
        }
        if (excluirObligatoriosDelCotejoCheckBox != null) {
            excluirObligatoriosDelCotejoCheckBox.setSelected(evento.isExcluirObligatoriosDelCotejo());
        }
        if (ultimaRondaSoloObligatoriosCheckBox != null) {
            ultimaRondaSoloObligatoriosCheckBox.setSelected(evento.isUltimaRondaSoloObligatorios());
            ultimaRondaSoloObligatoriosCheckBox.setDisable(evento.isExcluirObligatoriosDelCotejo());
        }
        notifyRuleChange();
        updateReglasVisibility(evento.getGallosPorPartido());
    }

    private void updateReglasVisibility(Integer gallosPorPartido) {
        boolean mostrar = gallosPorPartido != null && gallosPorPartido > 1;
        if (reglasCotejoPane != null) {
            reglasCotejoPane.setVisible(mostrar);
            reglasCotejoPane.setManaged(mostrar);
            reglasCotejoPane.setExpanded(mostrar);
        }
        if (reglasCotejoBox != null) {
            reglasCotejoBox.setVisible(mostrar);
            reglasCotejoBox.setManaged(mostrar);
        }
    }

    private ModoCotejo readModoCotejo() {
        if (modoRondasRadio == null) {
            return ModoCotejo.ALEATORIO;
        }
        if (modoRondasRadio.isSelected()) {
            return ModoCotejo.RONDAS;
        }
        return ModoCotejo.ALEATORIO;
    }

    private boolean isExcluirObligatoriosDelCotejo() {
        return excluirObligatoriosDelCotejoCheckBox != null && excluirObligatoriosDelCotejoCheckBox.isSelected();
    }

    private boolean isUltimaRondaSoloObligatorios() {
        return ultimaRondaSoloObligatoriosCheckBox != null
                && ultimaRondaSoloObligatoriosCheckBox.isSelected()
                && !isExcluirObligatoriosDelCotejo();
    }

    private void notifyRuleChange() {
        if (shellController != null && eventoActual != null) {
            shellController.actualizarInfoEvento(eventoActual, readModoCotejo());
        }
    }

    public ModoCotejo getModoCotejoSeleccionado() {
        return readModoCotejo();
    }

    private CotejoGuardado guardarCotejo(Evento evento, double tolerancia,
                                    List<Pelea> peleas, List<Gallo> gallosSinPeleaResultado) throws Exception {
        CotejoGuardado cotejo = new CotejoGuardado(
                null,
                evento.getId(),
                tolerancia,
                LocalDateTime.now().toString()
        );
        cotejo.setPeleas(toPeleasGuardadas(peleas));
        cotejo.setGallosSinPelea(toGallosSinPeleaGuardados(gallosSinPeleaResultado));
        cotejoRepository.guardar(cotejo);
        return cotejo;
    }

    private CotejoGuardado guardarOCrearCotejoActual() throws Exception {
        Double tolerancia = readTolerancia();
        double toleranciaFinal = tolerancia == null ? DEFAULT_TOLERANCIA : tolerancia;
        if (cotejoEditableActual != null && cotejoEditableActual.getId() != null) {
            cotejoEditableActual.setToleranciaGramos(toleranciaFinal);
            cotejoEditableActual.setPeleas(toPeleasGuardadas(peleasCotejo));
            cotejoEditableActual.setGallosSinPelea(toGallosSinPeleaGuardados(gallosSinPelea));
            cotejoRepository.actualizar(cotejoEditableActual);
            loadCotejosGuardados(eventoActual.getId());
            selectCotejoById(cotejoEditableActual.getId());
            return cotejoEditableActual;
        }

        CotejoGuardado cotejoGuardado = guardarCotejo(
                eventoActual,
                toleranciaFinal,
                new ArrayList<>(peleasCotejo),
                new ArrayList<>(gallosSinPelea));
        cotejoEditableActual = cotejoGuardado;
        loadCotejosGuardados(eventoActual.getId());
        selectCotejoById(cotejoGuardado.getId());
        return cotejoGuardado;
    }

    private List<PeleaGuardada> toPeleasGuardadas(List<Pelea> peleas) {
        List<PeleaGuardada> peleasGuardadas = new ArrayList<>();
        int orden = 1;
        for (Pelea pelea : peleas) {
            peleasGuardadas.add(new PeleaGuardada(
                    null,
                    null,
                    orden,
                    pelea.getGallo1().getId(),
                    pelea.getGallo2().getId(),
                    pelea.getDiferenciaPeso(),
                    pelea.getRonda()
            ));
            orden++;
        }
        return peleasGuardadas;
    }

    private List<GalloSinPeleaGuardado> toGallosSinPeleaGuardados(List<Gallo> gallos) {
        List<GalloSinPeleaGuardado> guardados = new ArrayList<>();
        for (Gallo gallo : gallos) {
            Long galloId = gallo == null ? null : gallo.getId();
            Integer ronda = galloId == null ? null : rondaPorGalloId.get(galloId);
            guardados.add(new GalloSinPeleaGuardado(null, null, galloId, ronda == null ? 1 : ronda));
        }
        return guardados;
    }

    private void mostrarDetallePelea(Pelea pelea) {
        if (pelea == null) {
            ocultarDetallePelea();
            return;
        }

        int index = peleasCotejo.indexOf(pelea);
        peleaSeleccionada.set(pelea);

        if (peleaDetallePane != null) {
            peleaDetallePane.setOpacity(0.0);
            peleaDetallePane.setTranslateX(18.0);
            peleaDetallePane.setVisible(true);
            peleaDetallePane.setManaged(true);
            animarDetallePelea(true);
        }
        aplicarTemaDetallePelea();
        if (peleaDetalleTitulo != null) {
            peleaDetalleTitulo.setText(index >= 0 ? "Detalle de pelea " + (index + 1) : "Detalle de pelea");
        }
        if (peleaDetalleResumenLabel != null) {
            peleaDetalleResumenLabel.setText(formatearPeleaResumen(pelea));
        }
        if (peleaDetalleDiferenciaLabel != null) {
            peleaDetalleDiferenciaLabel.setText(formatearDiferencia(pelea.getDiferenciaPeso()) + " g");
        }
        if (peleaDetallePartido1Label != null) {
            peleaDetallePartido1Label.setText(nombrePartidoConEntrada(pelea.getGallo1()));
        }
        if (peleaDetallePartido2Label != null) {
            peleaDetallePartido2Label.setText(nombrePartidoConEntrada(pelea.getGallo2()));
        }
        configurarComboPelea(peleaDetalleGallo1Combo, pelea.getGallo1());
        configurarComboPelea(peleaDetalleGallo2Combo, pelea.getGallo2());
        cargarCamposDetallePelea(pelea);
        actualizarBotonesDetalle(index >= 0);
    }

    private void abrirDetallePelea(Pelea pelea) {
        if (pelea == null) {
            return;
        }
        if (peleasCotejoTable != null) {
            int index = peleasCotejo.indexOf(pelea);
            if (index >= 0 && index < peleasCotejoFilas.size()) {
                peleasCotejoTable.getSelectionModel().select(index);
            }
        }
        mostrarDetallePelea(pelea);
    }

    private void ocultarDetallePelea() {
        peleaSeleccionada.set(null);
        aplicarTemaDetallePelea();
        animarDetallePelea(false);
        if (peleaDetalleTitulo != null) {
            peleaDetalleTitulo.setText("Detalle de pelea");
        }
        if (peleaDetalleResumenLabel != null) {
            peleaDetalleResumenLabel.setText("Selecciona una pelea para editarla.");
        }
        if (peleaDetalleDiferenciaLabel != null) {
            peleaDetalleDiferenciaLabel.setText("-");
        }
        if (peleaDetallePartido1Label != null) {
            peleaDetallePartido1Label.setText("-");
        }
        if (peleaDetallePartido2Label != null) {
            peleaDetallePartido2Label.setText("-");
        }
        limpiarCamposDetallePelea();
        if (peleaDetalleGallo1Combo != null) {
            peleaDetalleGallo1Combo.getSelectionModel().clearSelection();
        }
        if (peleaDetalleGallo2Combo != null) {
            peleaDetalleGallo2Combo.getSelectionModel().clearSelection();
        }
        actualizarBotonesDetalle(false);
    }

    private void animarDetallePelea(boolean mostrar) {
        if (peleaDetallePane == null) {
            return;
        }

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(180), peleaDetallePane);
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(180), peleaDetallePane);

        if (mostrar) {
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);
            translateTransition.setFromX(18.0);
            translateTransition.setToX(0.0);
        } else {
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.0);
            translateTransition.setFromX(0.0);
            translateTransition.setToX(18.0);
        }

        ParallelTransition transition = new ParallelTransition(fadeTransition, translateTransition);
        if (!mostrar) {
            transition.setOnFinished(event -> {
                peleaDetallePane.setVisible(false);
                peleaDetallePane.setManaged(false);
                peleaDetallePane.setOpacity(0.0);
                peleaDetallePane.setTranslateX(18.0);
            });
        }
        transition.play();
    }

    private void configurarComboGallo(ComboBox<Gallo> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setItems(gallosEvento);
        comboBox.setConverter(new javafx.util.StringConverter<Gallo>() {
            @Override
            public String toString(Gallo gallo) {
                return formatearGallo(gallo);
            }

            @Override
            public Gallo fromString(String string) {
                return null;
            }
        });
        aplicarEstiloComboDetalle(comboBox);
    }

    private void configurarComboPelea(ComboBox<Gallo> comboBox, Gallo seleccionado) {
        if (comboBox == null) {
            return;
        }
        comboBox.setItems(gallosEvento);
        comboBox.getSelectionModel().select(seleccionado);
        aplicarEstiloComboDetalle(comboBox);
    }

    private void cargarCamposDetallePelea(Pelea pelea) {
        if (pelea == null) {
            limpiarCamposDetallePelea();
            return;
        }
        cargarCamposGalloDetalle(1, pelea.getGallo1());
        cargarCamposGalloDetalle(2, pelea.getGallo2());
        if (peleaDetallePartido1Field != null) {
            peleaDetallePartido1Field.setText(nombrePartidoConEntrada(pelea.getGallo1()));
        }
        if (peleaDetallePartido2Field != null) {
            peleaDetallePartido2Field.setText(nombrePartidoConEntrada(pelea.getGallo2()));
        }
    }

    private void cargarCamposGalloDetalle(int numeroGallo, Gallo gallo) {
        if (gallo == null) {
            if (numeroGallo == 1) {
                if (peleaDetalleGallo1PesoField != null) {
                    peleaDetalleGallo1PesoField.clear();
                }
                if (peleaDetalleGallo1AnilloField != null) {
                    peleaDetalleGallo1AnilloField.clear();
                }
                if (peleaDetallePartido1Field != null) {
                    peleaDetallePartido1Field.clear();
                }
            } else {
                if (peleaDetalleGallo2PesoField != null) {
                    peleaDetalleGallo2PesoField.clear();
                }
                if (peleaDetalleGallo2AnilloField != null) {
                    peleaDetalleGallo2AnilloField.clear();
                }
                if (peleaDetallePartido2Field != null) {
                    peleaDetallePartido2Field.clear();
                }
            }
            return;
        }

        if (numeroGallo == 1) {
            if (peleaDetalleGallo1PesoField != null) {
                peleaDetalleGallo1PesoField.setText(formatearPesoSinDecimalCero(gallo.getPeso()));
            }
            if (peleaDetalleGallo1AnilloField != null) {
                peleaDetalleGallo1AnilloField.setText(valorTexto(gallo.getAnillo()));
            }
            if (peleaDetallePartido1Field != null) {
                peleaDetallePartido1Field.setText(nombrePartidoConEntrada(gallo));
            }
        } else {
            if (peleaDetalleGallo2PesoField != null) {
                peleaDetalleGallo2PesoField.setText(formatearPesoSinDecimalCero(gallo.getPeso()));
            }
            if (peleaDetalleGallo2AnilloField != null) {
                peleaDetalleGallo2AnilloField.setText(valorTexto(gallo.getAnillo()));
            }
            if (peleaDetallePartido2Field != null) {
                peleaDetallePartido2Field.setText(nombrePartidoConEntrada(gallo));
            }
        }
    }

    private void limpiarCamposDetallePelea() {
        if (peleaDetalleGallo1PesoField != null) {
            peleaDetalleGallo1PesoField.clear();
        }
        if (peleaDetalleGallo1AnilloField != null) {
            peleaDetalleGallo1AnilloField.clear();
        }
        if (peleaDetallePartido1Field != null) {
            peleaDetallePartido1Field.clear();
        }
        if (peleaDetalleGallo2PesoField != null) {
            peleaDetalleGallo2PesoField.clear();
        }
        if (peleaDetalleGallo2AnilloField != null) {
            peleaDetalleGallo2AnilloField.clear();
        }
        if (peleaDetallePartido2Field != null) {
            peleaDetallePartido2Field.clear();
        }
    }

    private String valorTexto(String texto) {
        return texto == null ? "" : texto.trim();
    }

    public void aplicarTemaDetallePelea() {
        boolean oscuro = shellController == null || shellController.isDarkThemeActive();
        String colorPrincipal = oscuro ? "#FFFFFF" : "#191C1E";
        String colorSecundario = oscuro ? "#FFFFFF" : "#535F70";

        aplicarColorTexto(peleaDetalleTitulo, colorPrincipal);
        aplicarColorTexto(peleaDetalleResumenLabel, colorSecundario);
        aplicarColorTexto(peleaDetalleGallo1Label, colorPrincipal);
        aplicarColorTexto(peleaDetallePeso1Label, colorPrincipal);
        aplicarColorTexto(peleaDetalleAnillo1Label, colorPrincipal);
        aplicarColorTexto(peleaDetallePartido1TextoLabel, colorPrincipal);
        aplicarColorTexto(peleaDetalleGallo2Label, colorPrincipal);
        aplicarColorTexto(peleaDetallePeso2Label, colorPrincipal);
        aplicarColorTexto(peleaDetalleAnillo2Label, colorPrincipal);
        aplicarColorTexto(peleaDetallePartido2TextoLabel, colorPrincipal);
        aplicarColorTexto(peleaDetalleDifPesoLabel, colorPrincipal);
        aplicarColorTexto(peleaDetallePartido1Label, colorPrincipal);
        aplicarColorTexto(peleaDetallePartido2Label, colorPrincipal);
        aplicarColorTexto(peleaDetalleDiferenciaLabel, colorPrincipal);

        aplicarEstiloComboDetalle(peleaDetalleGallo1Combo);
        aplicarEstiloComboDetalle(peleaDetalleGallo2Combo);
    }

    private void aplicarColorTexto(Label label, String color) {
        if (label == null) {
            return;
        }
        label.setStyle("-fx-text-fill: " + color + ";");
    }

    private void aplicarEstiloComboDetalle(ComboBox<Gallo> comboBox) {
        if (comboBox == null) {
            return;
        }

        boolean oscuro = shellController == null || shellController.isDarkThemeActive();
        String color = oscuro ? "#FFFFFF" : "#191C1E";
        comboBox.setButtonCell(crearCeldaGallo(color));
        comboBox.setCellFactory(listView -> crearCeldaGallo(color));
        comboBox.setStyle("-fx-text-fill: " + color + ";");
    }

    private ListCell<Gallo> crearCeldaGallo(String color) {
        return new ListCell<Gallo>() {
            @Override
            protected void updateItem(Gallo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatearGallo(item));
                }
                setTextFill(Color.web(color));
            }
        };
    }

    private void actualizarBotonesDetalle(boolean habilitar) {
        if (peleaEliminarButton != null) {
            peleaEliminarButton.setDisable(!habilitar);
        }
        if (peleaAplicarButton != null) {
            peleaAplicarButton.setDisable(!habilitar);
        }
    }

    private Button crearBotonEliminarPelea() {
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
        boton.setTooltip(new Tooltip("Eliminar pelea"));
        return boton;
    }

    @FXML
    private void cerrarDetallePelea() {
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().clearSelection();
        }
        ocultarDetallePelea();
    }

    @FXML
    private void aplicarEdicionPelea() {
        int index = peleasCotejoTable == null ? -1 : peleasCotejoTable.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= peleasCotejo.size()) {
            shellController.setStatus("Selecciona una pelea para editar.");
            return;
        }

        Gallo gallo1 = peleaDetalleGallo1Combo == null ? null : peleaDetalleGallo1Combo.getSelectionModel().getSelectedItem();
        Gallo gallo2 = peleaDetalleGallo2Combo == null ? null : peleaDetalleGallo2Combo.getSelectionModel().getSelectedItem();
        if (gallo1 == null || gallo2 == null) {
            shellController.setStatus("Selecciona los dos gallos para la pelea.");
            return;
        }

        String partido1Nombre = leerTextoObligatorio(peleaDetallePartido1Field, "el nombre del partido 1");
        String partido2Nombre = leerTextoObligatorio(peleaDetallePartido2Field, "el nombre del partido 2");
        Double peso1 = leerPesoObligatorio(peleaDetalleGallo1PesoField, "el peso del gallo 1");
        Double peso2 = leerPesoObligatorio(peleaDetalleGallo2PesoField, "el peso del gallo 2");
        if (partido1Nombre == null || partido2Nombre == null || peso1 == null || peso2 == null) {
            return;
        }

        if (!esPeleaEditableValida(gallo1, gallo2, index, partido1Nombre, partido2Nombre, peso1, peso2)) {
            return;
        }

        try {
            actualizarGalloDesdeCampos(gallo1, peso1, peleaDetalleGallo1AnilloField);
            actualizarGalloDesdeCampos(gallo2, peso2, peleaDetalleGallo2AnilloField);
            actualizarPartidoDesdeCampo(gallo1.getPartidoId(), partido1Nombre);
            actualizarPartidoDesdeCampo(gallo2.getPartidoId(), partido2Nombre);
            recargarPartidosEvento();
            Pelea peleaActual = peleasCotejo.get(index);
            reemplazarPelea(index, crearPelea(gallo1, gallo2, peleaActual == null ? 1 : peleaActual.getRonda()));
            persistirCotejoEditable("Pelea actualizada.");
        } catch (Exception e) {
            shellController.showError("Error al guardar los cambios de la pelea", e);
        }
    }

    @FXML
    private void eliminarPeleaSeleccionada() {
        int index = peleasCotejoTable == null ? -1 : peleasCotejoTable.getSelectionModel().getSelectedIndex();
        eliminarPeleaEnIndice(index);
    }

    private void eliminarPeleaEnIndice(int index) {
        if (index < 0 || index >= peleasCotejo.size()) {
            shellController.setStatus("Selecciona una pelea para eliminar.");
            return;
        }

        peleasCotejo.remove(index);
        if (peleasCotejo.isEmpty()) {
            ocultarDetallePelea();
        } else {
            int nuevoIndice = Math.min(index, peleasCotejo.size() - 1);
            if (peleasCotejoTable != null) {
                peleasCotejoTable.getSelectionModel().select(nuevoIndice);
            }
        }
        if (peleasCotejoTable != null) {
            peleasCotejoTable.refresh();
        }
        persistirCotejoEditable("Pelea eliminada.");
    }

    private void reemplazarPelea(int index, Pelea nuevaPelea) {
        if (index < 0 || index >= peleasCotejo.size() || nuevaPelea == null) {
            return;
        }
        peleasCotejo.set(index, nuevaPelea);
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().select(index);
            peleasCotejoTable.refresh();
        }
        mostrarDetallePelea(nuevaPelea);
    }

    private void persistirCotejoEditable(String mensajeExito) {
        if (cotejoEditableActual == null || eventoActual == null) {
            shellController.setStatus(mensajeExito + " Cambios en memoria; usa Guardar cotejo para persistir.");
            return;
        }

        try {
            cotejoEditableActual.setToleranciaGramos(readTolerancia() == null ? cotejoEditableActual.getToleranciaGramos() : readTolerancia());
            cotejoEditableActual.setPeleas(toPeleasGuardadas(peleasCotejo));
            cotejoEditableActual.setGallosSinPelea(toGallosSinPeleaGuardados(gallosSinPelea));
            cotejoRepository.actualizar(cotejoEditableActual);
            loadCotejosGuardados(eventoActual.getId());
            selectCotejoById(cotejoEditableActual.getId());
            shellController.setStatus(mensajeExito);
        } catch (Exception e) {
            shellController.showError("Error al guardar cambios del cotejo", e);
        }
    }

    private boolean esPeleaEditableValida(Gallo gallo1,
                                          Gallo gallo2,
                                          int indiceActual,
                                          String partido1Nombre,
                                          String partido2Nombre,
                                          double peso1,
                                          double peso2) {
        if (gallo1.getId() != null && gallo1.getId().equals(gallo2.getId())) {
            shellController.setStatus("Una pelea no puede usar el mismo gallo dos veces.");
            return false;
        }
        if (gallo1.getPartidoId() != null && gallo1.getPartidoId().equals(gallo2.getPartidoId())) {
            shellController.setStatus("Los gallos no pueden pertenecer al mismo partido.");
            return false;
        }
        if (partido1Nombre.equalsIgnoreCase(partido2Nombre)) {
            shellController.setStatus("Los gallos no pueden compartir el mismo nombre de partido.");
            return false;
        }
        if (existeRestriccionManualProhibida(gallo1, gallo2)) {
            shellController.setStatus("Esa pareja viola una restriccion entre partidos.");
            return false;
        }
        Double tolerancia = readTolerancia();
        double diferencia = Math.abs(peso1 - peso2);
        if (tolerancia != null && diferencia > tolerancia) {
            shellController.setStatus("La diferencia de peso supera la tolerancia.");
            return false;
        }
        if (galloUsadoEnOtraPelea(gallo1.getId(), indiceActual) || galloUsadoEnOtraPelea(gallo2.getId(), indiceActual)) {
            shellController.setStatus("Uno de los gallos ya está asignado a otra pelea.");
            return false;
        }
        return true;
    }

    private void actualizarGalloDesdeCampos(Gallo gallo, double peso, TextField anilloField) throws java.sql.SQLException {
        if (gallo == null) {
            return;
        }
        gallo.setPeso(peso);
        gallo.setAnillo(valorTexto(anilloField == null ? null : anilloField.getText()));
        if (galloRepository != null) {
            galloRepository.actualizar(gallo);
        }
    }

    private void actualizarPartidoDesdeCampo(Long partidoId, String nuevoNombre) throws java.sql.SQLException {
        if (partidoId == null || partidoRepository == null) {
            return;
        }
        Partido partido = partidoRepository.buscarPorId(partidoId);
        if (partido == null) {
            return;
        }
        partido.setNombre(nuevoNombre);
        partidoRepository.actualizar(partido);
    }

    private void recargarPartidosEvento() {
        if (eventoActual == null || partidoRepository == null) {
            return;
        }
        try {
            partidos.setAll(partidoRepository.listarPorEvento(eventoActual.getId()));
            cargarNombresPartidos(eventoActual);
            calcularRondasGallosEvento();
            if (peleaDetalleGallo1Combo != null) {
                peleaDetalleGallo1Combo.setButtonCell(crearCeldaGallo(shellController == null || shellController.isDarkThemeActive() ? "#FFFFFF" : "#191C1E"));
                peleaDetalleGallo1Combo.setValue(peleaDetalleGallo1Combo.getValue());
            }
            if (peleaDetalleGallo2Combo != null) {
                peleaDetalleGallo2Combo.setButtonCell(crearCeldaGallo(shellController == null || shellController.isDarkThemeActive() ? "#FFFFFF" : "#191C1E"));
                peleaDetalleGallo2Combo.setValue(peleaDetalleGallo2Combo.getValue());
            }
        } catch (Exception e) {
            shellController.showError("Error al recargar los partidos", e);
        }
    }

    public void refrescarPartidosEventoActual() {
        recargarPartidosEvento();
    }

    private String leerTextoObligatorio(TextField field, String campo) {
        if (field == null) {
            shellController.setStatus("No se pudo leer " + campo + ".");
            return null;
        }
        String texto = valorTexto(field.getText());
        if (texto.isEmpty()) {
            shellController.setStatus("Debes ingresar " + campo + ".");
            return null;
        }
        return texto;
    }

    private Double leerPesoObligatorio(TextField field, String campo) {
        if (field == null) {
            shellController.setStatus("No se pudo leer " + campo + ".");
            return null;
        }
        String texto = valorTexto(field.getText());
        if (texto.isEmpty()) {
            shellController.setStatus("Debes ingresar " + campo + ".");
            return null;
        }
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            shellController.setStatus("El " + campo + " debe ser numerico.");
            return null;
        }
    }

    private boolean existeRestriccionManualProhibida(Gallo gallo1, Gallo gallo2) {
        for (RestriccionPartido restriccion : restricciones) {
            if (RestriccionPartido.TIPO_PROHIBIDO.equals(restriccion.getTipo())
                    && restriccion.aplicaEntre(gallo1.getPartidoId(), gallo2.getPartidoId())) {
                return true;
            }
        }
        return false;
    }

    private boolean galloUsadoEnOtraPelea(Long galloId, int indiceActual) {
        if (galloId == null) {
            return false;
        }
        for (int i = 0; i < peleasCotejo.size(); i++) {
            if (i == indiceActual) {
                continue;
            }
            Pelea pelea = peleasCotejo.get(i);
            if (galloId.equals(pelea.getGallo1().getId()) || galloId.equals(pelea.getGallo2().getId())) {
                return true;
            }
        }
        return false;
    }

    private Pelea crearPelea(Gallo gallo1, Gallo gallo2) {
        return crearPelea(gallo1, gallo2, 1);
    }

    private Pelea crearPelea(Gallo gallo1, Gallo gallo2, int ronda) {
        return new Pelea(gallo1, gallo2, diferenciaPeso(gallo1, gallo2), ronda);
    }

    private double diferenciaPeso(Gallo gallo1, Gallo gallo2) {
        return Math.abs(gallo1.getPeso() - gallo2.getPeso());
    }

    private String formatearPeleaResumen(Pelea pelea) {
        return formatearGallo(pelea.getGallo1()) + " vs " + formatearGallo(pelea.getGallo2());
    }

    private String formatearAnilloCompacto(Gallo gallo) {
        if (gallo == null) {
            return "-";
        }
        String anillo = gallo.getAnillo() == null || gallo.getAnillo().trim().isEmpty() ? "-" : gallo.getAnillo().trim();
        return anillo;
    }

    private String formatearPesoCompacto(double peso) {
        return formatearPesoSinDecimalCero(peso);
    }

    private String formatearDiferenciaCompacta(double diferenciaPeso) {
        return formatearPesoSinDecimalCero(diferenciaPeso);
    }

    private String formatearPesoSinDecimalCero(double peso) {
        if (Math.abs(peso - Math.rint(peso)) < 0.0001) {
            return String.valueOf((long) Math.rint(peso));
        }
        return formatearPeso(peso);
    }

    private String formatearGallo(Gallo gallo) {
        if (gallo == null) {
            return "-";
        }
        String partido = nombrePartidoConEntrada(gallo);
        String anillo = gallo.getAnillo() == null || gallo.getAnillo().trim().isEmpty() ? "-" : gallo.getAnillo().trim();
        return gallo.getId() + " | " + formatearPeso(gallo.getPeso()) + " | " + anillo + " | " + partido;
    }

    private String formatearGalloSinId(Gallo gallo) {
        if (gallo == null) {
            return "-";
        }
        String partido = nombrePartidoConEntrada(gallo);
        String anillo = gallo.getAnillo() == null || gallo.getAnillo().trim().isEmpty() ? "-" : gallo.getAnillo().trim();
        return partido + " | " + formatearPeso(gallo.getPeso()) + " | " + anillo;
    }

    private String nombrePartidoConEntrada(Gallo gallo) {
        if (gallo == null) {
            return "-";
        }
        int gallosPorPartido = eventoActual == null ? 0 : eventoActual.getGallosPorPartido();
        return EntradaGallo.formatearPartidoConEntrada(
                gallo,
                nombrePartido(gallo.getPartidoId()),
                gallosPorPartido,
                partidoTieneMultiplesEntradas(gallo));
    }

    private boolean partidoTieneMultiplesEntradas(Gallo gallo) {
        if (gallo == null || gallo.getPartidoId() == null || eventoActual == null || eventoActual.getGallosPorPartido() <= 0) {
            return false;
        }
        int gallosDelPartido = 0;
        for (Gallo galloEvento : gallosEvento) {
            if (galloEvento != null && gallo.getPartidoId().equals(galloEvento.getPartidoId())) {
                gallosDelPartido++;
            }
        }
        return gallosDelPartido > eventoActual.getGallosPorPartido();
    }

    private void calcularRondasGallosEvento() {
        rondaPorGalloId.clear();
        Map<Long, Integer> contadorPorPartido = new HashMap<>();
        for (Gallo gallo : gallosEvento) {
            if (gallo == null || gallo.getId() == null || gallo.getPartidoId() == null) {
                continue;
            }
            int ronda = contadorPorPartido.getOrDefault(gallo.getPartidoId(), 0) + 1;
            contadorPorPartido.put(gallo.getPartidoId(), ronda);
            rondaPorGalloId.put(gallo.getId(), ronda);
        }
    }

    private String formatearPeso(double peso) {
        return String.format(java.util.Locale.US, "%.1f", peso);
    }

    private TableRow<PeleaFila> crearFilaArrastrable() {
        TableRow<PeleaFila> row = new TableRow<PeleaFila>() {
            @Override
            protected void updateItem(PeleaFila item, boolean empty) {
                super.updateItem(item, empty);
                setDropIndicators(this, false, false);
            }
        };

        row.setOnDragDetected(event -> {
            if (row.isEmpty()) {
                return;
            }
            setDragActive(row, true);
            Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(row.getIndex()));
            dragboard.setContent(content);
            event.consume();
        });

        row.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString() && !row.isEmpty()) {
                try {
                    int sourceIndex = Integer.parseInt(dragboard.getString());
                    if (sourceIndex != row.getIndex()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        boolean dropTop = event.getY() < row.getHeight() / 2.0;
                        setDropIndicators(row, dropTop, !dropTop);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignorar datos no válidos.
                }
            }
            event.consume();
        });

        row.setOnDragExited(event -> {
            setDropIndicators(row, false, false);
            event.consume();
        });

        row.setOnDragDone(event -> {
            setDragActive(row, false);
            event.consume();
        });

        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                PeleaFila fila = row.getItem();
                abrirDetallePelea(fila == null ? null : fila.getPelea());
                event.consume();
            }
        });

        row.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasString() && !row.isEmpty()) {
                try {
                    int sourceIndex = Integer.parseInt(dragboard.getString());
                    int insertionIndex = event.getY() < row.getHeight() / 2.0
                            ? row.getIndex()
                            : row.getIndex() + 1;
                    moverPeleaPorArrastre(sourceIndex, insertionIndex);
                    success = true;
                } catch (NumberFormatException ignored) {
                    success = false;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return row;
    }

    private Button crearBotonEditarPelea() {
        SVGPath icono = new SVGPath();
        icono.setContent("M3 17.25V21h3.75L19.81 7.94l-3.75-3.75L3 17.25zm2.92 2.83H5v-.92L14.06 8.1l.92.92L5.92 20.08zM20.71 6.04c.39-.39.39-1.02 0-1.41L19.37 3.29c-.39-.39-1.02-.39-1.41 0l-1.06 1.06 3.75 3.75 1.06-1.06z");
        icono.setFill(Color.WHITE);
        icono.setScaleX(0.78);
        icono.setScaleY(0.78);

        Button boton = new Button();
        boton.getStyleClass().add("primary-action");
        boton.getStyleClass().add("fight-action-button");
        boton.setGraphic(icono);
        boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        boton.setPrefWidth(34.0);
        boton.setMinWidth(34.0);
        boton.setMaxWidth(34.0);
        boton.setStyle("-fx-padding: 0;");
        boton.setTooltip(new Tooltip("Editar pelea"));
        return boton;
    }

    private void setDragActive(TableRow<PeleaFila> row, boolean active) {
        if (row == null) {
            return;
        }
        row.pseudoClassStateChanged(DRAG_ACTIVE, active);
    }

    private void setDropIndicators(TableRow<PeleaFila> row, boolean top, boolean bottom) {
        if (row == null) {
            return;
        }
        row.pseudoClassStateChanged(DROP_TOP, top);
        row.pseudoClassStateChanged(DROP_BOTTOM, bottom);
    }

    private void moverPeleaPorArrastre(int sourceIndex, int insertionIndex) {
        if (sourceIndex < 0 || sourceIndex >= peleasCotejo.size()) {
            return;
        }
        if (insertionIndex < 0 || insertionIndex > peleasCotejo.size()) {
            return;
        }

        Pelea pelea = peleasCotejo.remove(sourceIndex);
        if (sourceIndex < insertionIndex) {
            insertionIndex--;
        }
        if (insertionIndex < 0) {
            insertionIndex = 0;
        }
        if (insertionIndex > peleasCotejo.size()) {
            insertionIndex = peleasCotejo.size();
        }
        peleasCotejo.add(insertionIndex, pelea);
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().select(insertionIndex);
            peleasCotejoTable.refresh();
        }
        persistirCotejoEditable("Orden de pelea actualizado.");
    }

    private void rebuildPeleasCotejoFilas() {
        List<PeleaFila> filas = new ArrayList<>();
        for (Pelea pelea : peleasCotejo) {
            filas.add(new PeleaFila(pelea, pelea == null ? 1 : pelea.getRonda()));
        }
        peleasCotejoFilas.setAll(filas);
    }

    private static final class PeleaFila {
        private final Pelea pelea;
        private final int numeroRondaMostrada;

        private PeleaFila(Pelea pelea, int numeroRondaMostrada) {
            this.pelea = pelea;
            this.numeroRondaMostrada = numeroRondaMostrada;
        }

        private Pelea getPelea() {
            return pelea;
        }

        private int getNumeroRondaMostrada() {
            return numeroRondaMostrada;
        }
    }

    private void cargarCotejoSeleccionadoEnTablaPrincipal() {
        CotejoGuardado selectedCotejo = getCotejoSeleccionado();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado.");
            return;
        }

        try {
            CotejoGuardado detalle = cotejoRepository.cargarDetalle(selectedCotejo.getId());
            if (detalle == null) {
                shellController.setStatus("No se encontro el cotejo guardado.");
                return;
            }

            List<Pelea> peleasCargadas = toPeleasDesdeGuardado(detalle.getPeleas());
            List<Gallo> gallosSinPeleaCargados = toGallosSinPeleaDesdeGuardado(detalle.getGallosSinPelea());
            cargarRondasDesdeCotejoGuardado(detalle);
            peleasCotejo.setAll(peleasCargadas);
            gallosSinPelea.setAll(gallosSinPeleaCargados);
            cotejoEditableActual = detalle;
            if (toleranciaSpinner != null) {
                toleranciaSpinner.getValueFactory().setValue(detalle.getToleranciaGramos());
            }
            if (peleasCotejoTable != null) {
                peleasCotejoTable.getSelectionModel().clearSelection();
            }
            ocultarDetallePelea();
            shellController.setStatus("Cotejo guardado cargado en la tabla principal.");
        } catch (Exception e) {
            shellController.showError("Error al cargar cotejo guardado", e);
        }
    }

    private List<Pelea> toPeleasDesdeGuardado(List<PeleaGuardada> peleasGuardadas) throws Exception {
        List<Pelea> peleas = new ArrayList<>();
        if (peleasGuardadas == null) {
            return peleas;
        }
        for (PeleaGuardada peleaGuardada : peleasGuardadas) {
            if (peleaGuardada == null) {
                continue;
            }
            Gallo gallo1 = galloRepository.buscarPorId(peleaGuardada.getGallo1Id());
            Gallo gallo2 = galloRepository.buscarPorId(peleaGuardada.getGallo2Id());
            if (gallo1 == null || gallo2 == null) {
                throw new IllegalStateException("No se pudo reconstruir una pelea guardada por datos faltantes.");
            }
            peleas.add(new Pelea(gallo1, gallo2, peleaGuardada.getDiferenciaPeso(), peleaGuardada.getRonda()));
        }
        return peleas;
    }

    private void cargarRondasDesdeCotejoGuardado(CotejoGuardado detalle) {
        rondaPorGalloId.clear();
        if (detalle == null) {
            return;
        }
        for (PeleaGuardada pelea : detalle.getPeleas()) {
            if (pelea == null) {
                continue;
            }
            if (pelea.getGallo1Id() != null) {
                rondaPorGalloId.put(pelea.getGallo1Id(), pelea.getRonda());
            }
            if (pelea.getGallo2Id() != null) {
                rondaPorGalloId.put(pelea.getGallo2Id(), pelea.getRonda());
            }
        }
        for (GalloSinPeleaGuardado galloSinPelea : detalle.getGallosSinPelea()) {
            if (galloSinPelea != null && galloSinPelea.getGalloId() != null) {
                rondaPorGalloId.put(galloSinPelea.getGalloId(), galloSinPelea.getRonda());
            }
        }
    }

    private List<Gallo> toGallosSinPeleaDesdeGuardado(List<GalloSinPeleaGuardado> guardados) throws Exception {
        List<Gallo> gallos = new ArrayList<>();
        if (guardados == null) {
            return gallos;
        }
        for (GalloSinPeleaGuardado guardado : guardados) {
            if (guardado == null) {
                continue;
            }
            Gallo gallo = galloRepository.buscarPorId(guardado.getGalloId());
            if (gallo != null) {
                gallos.add(gallo);
            }
        }
        return gallos;
    }

    @FXML
    private void exportarPdfCotejoGuardado() {
        CotejoGuardado selectedCotejo = getCotejoSeleccionado();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado para exportar.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF del cotejo");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName("cotejo-" + selectedCotejo.getId() + ".pdf");

            Stage stage = (Stage) cotejoGuardadoTable.getScene().getWindow();

            File selectedFile = fileChooser.showSaveDialog(stage);
            if (selectedFile == null) {
                shellController.setStatus("Exportacion cancelada.");
                return;
            }

            File destino = ensurePdfExtension(selectedFile);
            pdfCotejoService.exportar(selectedCotejo.getId(), destino.toPath());
            shellController.setStatus("PDF exportado: " + destino.getAbsolutePath());
        } catch (Exception e) {
            shellController.showError("Error al exportar PDF", e);
        }
    }

    @FXML
    private void imprimirCotejoGuardado() {
        CotejoGuardado selectedCotejo = getCotejoSeleccionado();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado para imprimir.");
            return;
        }
        mostrarPanelOpcionesImpresion(selectedCotejo);
        shellController.setStatus("Selecciona una opcion de impresion.");
    }

    @FXML
    private void eliminarCotejoGuardado() {
        CotejoGuardado selectedCotejo = getCotejoSeleccionado();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado para eliminar.");
            return;
        }

        try {
            cotejoRepository.eliminar(selectedCotejo.getId());
            if (eventoActual != null) {
                loadCotejosGuardados(eventoActual.getId());
            }
            shellController.setStatus("Cotejo guardado eliminado.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar cotejo guardado", e);
        }
    }

    @FXML
    private void agregarRestriccion() {
        if (eventoActual == null) {
            shellController.setStatus("Selecciona un evento antes de agregar una restriccion.");
            return;
        }

        Partido origen = restriccionPartidoOrigenCombo.getSelectionModel().getSelectedItem();
        Partido destino = restriccionPartidoDestinoCombo.getSelectionModel().getSelectedItem();
        if (origen == null || destino == null) {
            shellController.setStatus("Selecciona los dos partidos para la restriccion.");
            return;
        }
        if (origen.getId().equals(destino.getId())) {
            shellController.setStatus("No puedes restringir un partido contra si mismo.");
            return;
        }
        if (existeRestriccionLogica(origen.getId(), destino.getId())) {
            shellController.setStatus("Ya existe una restriccion entre esos partidos.");
            return;
        }

        try {
            RestriccionPartido restriccion = new RestriccionPartido(
                    null,
                    eventoActual.getId(),
                    origen.getId(),
                    destino.getId(),
                    RestriccionPartido.TIPO_PROHIBIDO
            );
            restriccionPartidoRepository.insertar(restriccion);
            loadRestricciones(eventoActual.getId());
            clearRestriccionFields();
            shellController.setStatus("Restriccion agregada.");
        } catch (Exception e) {
            shellController.showError("Error al agregar restriccion", e);
        }
    }

    @FXML
    private void eliminarRestriccion() {
        RestriccionPartido selectedRestriccion = restriccionList.getSelectionModel().getSelectedItem();
        if (selectedRestriccion == null) {
            shellController.setStatus("Selecciona una restriccion para eliminar.");
            return;
        }

        try {
            restriccionPartidoRepository.eliminar(selectedRestriccion.getId());
            if (eventoActual != null) {
                loadRestricciones(eventoActual.getId());
            }
            clearRestriccionFields();
            shellController.setStatus("Restriccion eliminada.");
        } catch (Exception e) {
            shellController.showError("Error al eliminar restriccion", e);
        }
    }

    private boolean existeRestriccionLogica(Long partidoId1, Long partidoId2) {
        for (RestriccionPartido restriccion : restricciones) {
            if (RestriccionPartido.TIPO_PROHIBIDO.equals(restriccion.getTipo())
                    && restriccion.aplicaEntre(partidoId1, partidoId2)) {
                return true;
            }
        }
        return false;
    }

    private void clearRestriccionFields() {
        if (restriccionPartidoOrigenCombo != null) {
            restriccionPartidoOrigenCombo.getSelectionModel().clearSelection();
        }
        if (restriccionPartidoDestinoCombo != null) {
            restriccionPartidoDestinoCombo.getSelectionModel().clearSelection();
        }
        if (restriccionList != null) {
            restriccionList.getSelectionModel().clearSelection();
        }
    }

    private String nombrePartido(Long partidoId) {
        if (partidoId == null) {
            return "-";
        }
        String nombre = nombresPartidosEvento.get(partidoId);
        if (nombre != null && !nombre.trim().isEmpty()) {
            return nombre;
        }
        for (Partido partido : partidos) {
            if (partidoId.equals(partido.getId())) {
                return partido.getNombre();
            }
        }
        return String.valueOf(partidoId);
    }

    private String formatearDiferencia(double diferencia) {
        return String.format(java.util.Locale.US, "%.1f", diferencia);
    }

    private void selectCotejoById(Long id) {
        if (id == null || cotejoGuardadoTable == null) {
            return;
        }
        for (CotejoGuardado cotejo : cotejosGuardados) {
            if (id.equals(cotejo.getId())) {
                cotejoGuardadoTable.getSelectionModel().select(cotejo);
                return;
            }
        }
    }

    private CotejoGuardado getCotejoSeleccionado() {
        if (cotejoGuardadoTable == null) {
            return null;
        }
        return cotejoGuardadoTable.getSelectionModel().getSelectedItem();
    }

    private void mostrarPanelOpcionesImpresion(CotejoGuardado cotejo) {
        cotejoImpresionActual = cotejo;
        if (opcionesImpresionTituloLabel != null && cotejo != null && cotejo.getId() != null) {
            opcionesImpresionTituloLabel.setText("Imprimir cotejo #" + cotejo.getId() + ":");
        }
        if (opcionesImpresionPane != null) {
            opcionesImpresionPane.setVisible(true);
            opcionesImpresionPane.setManaged(true);
        }
    }

    @FXML
    private void cerrarPanelOpcionesImpresion() {
        if (opcionesImpresionPane != null) {
            opcionesImpresionPane.setVisible(false);
            opcionesImpresionPane.setManaged(false);
        }
        cotejoImpresionActual = null;
    }

    @FXML
    private void imprimirHojaPublico() {
        if (!validarCotejoParaImpresion()) {
            return;
        }
        shellController.setStatus("Pendiente: imprimir hoja para publico del cotejo #" + cotejoImpresionActual.getId());
    }

    @FXML
    private void imprimirHojaJuez() {
        if (!validarCotejoParaImpresion()) {
            return;
        }
        shellController.setStatus("Pendiente: imprimir hoja para juez del cotejo #" + cotejoImpresionActual.getId());
    }

    @FXML
    private void imprimirScore() {
        if (!validarCotejoParaImpresion()) {
            return;
        }
        shellController.setStatus("Pendiente: imprimir score del cotejo #" + cotejoImpresionActual.getId());
    }

    @FXML
    private void imprimirListaPartidos() {
        if (!validarCotejoParaImpresion()) {
            return;
        }
        shellController.setStatus("Pendiente: imprimir lista de partidos del cotejo #" + cotejoImpresionActual.getId());
    }

    private boolean validarCotejoParaImpresion() {
        if (cotejoImpresionActual == null || cotejoImpresionActual.getId() == null) {
            shellController.setStatus("Primero selecciona o guarda un cotejo para imprimir.");
            return false;
        }
        return true;
    }

    private String formatearFechaCotejo(String fechaGeneracion) {
        if (fechaGeneracion == null || fechaGeneracion.trim().isEmpty()) {
            return "-";
        }
        try {
            LocalDateTime fecha = LocalDateTime.parse(fechaGeneracion.trim());
            return fecha.format(COTEJO_FECHA_FORMATTER);
        } catch (Exception e) {
            return fechaGeneracion;
        }
    }

    private File ensurePdfExtension(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            return file;
        }
        Path parent = file.toPath().getParent();
        if (parent == null) {
            return new File(file.getAbsolutePath() + ".pdf");
        }
        return parent.resolve(file.getName() + ".pdf").toFile();
    }
}
