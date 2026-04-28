package com.cotejador.app.desktop;

import com.cotejador.app.core.cotejo.MotorCotejo;
import com.cotejador.app.core.cotejo.OrdenadorPeleas;
import com.cotejador.app.core.cotejo.ParametrosCotejo;
import com.cotejador.app.core.cotejo.ParametrosOrdenamiento;
import com.cotejador.app.core.cotejo.Pelea;
import com.cotejador.app.core.cotejo.ResultadoCotejo;
import com.cotejador.app.core.model.CotejoGuardado;
import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.ModoCotejo;
import com.cotejador.app.core.model.GalloSinPeleaGuardado;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.core.model.PeleaGuardada;
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
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.css.PseudoClass;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PeleasController {
    private static final PseudoClass DROP_TOP = PseudoClass.getPseudoClass("drop-top");
    private static final PseudoClass DROP_BOTTOM = PseudoClass.getPseudoClass("drop-bottom");
    private static final PseudoClass DRAG_ACTIVE = PseudoClass.getPseudoClass("drag-active");
    private static final double DEFAULT_TOLERANCIA = 80.0;
    private static final double MIN_TOLERANCIA = 0.0;
    private static final double MAX_TOLERANCIA = 10000.0;
    private static final double TOLERANCIA_STEP = 1.0;
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
    private final ObservableList<Gallo> gallosSinPelea = FXCollections.observableArrayList();
    private final ObservableList<Gallo> gallosEvento = FXCollections.observableArrayList();
    private final ObservableList<RestriccionPartido> restricciones = FXCollections.observableArrayList();
    private final ObservableList<CotejoGuardado> cotejosGuardados = FXCollections.observableArrayList();
    private final ObservableList<PeleaGuardada> peleasGuardadas = FXCollections.observableArrayList();
    private final ObservableList<GalloSinPeleaGuardado> gallosSinPeleaGuardados = FXCollections.observableArrayList();
    private final ObjectProperty<Pelea> peleaSeleccionada = new SimpleObjectProperty<>();
    private final Map<Long, String> nombresPartidosEvento = new HashMap<>();
    private CotejoGuardado cotejoEditableActual;

    @FXML
    private Spinner<Double> toleranciaSpinner;
    @FXML
    private TextField partidosPrioritariosField;
    @FXML
    private ComboBox<String> modoCotejoComboBox;
    @FXML
    private CheckBox ultimaRondaSoloObligatoriosCheckBox;
    @FXML
    private CheckBox excluirObligatoriosDelCotejoCheckBox;
    @FXML
    private VBox reglasCotejoBox;
    @FXML
    private TableView<Pelea> peleasCotejoTable;
    @FXML
    private TableColumn<Pelea, Number> peleaNumeroColumn;
    @FXML
    private TableColumn<Pelea, String> peleaGallo1Column;
    @FXML
    private TableColumn<Pelea, String> peleaPeso1Column;
    @FXML
    private TableColumn<Pelea, String> peleaGallo2Column;
    @FXML
    private TableColumn<Pelea, String> peleaPeso2Column;
    @FXML
    private TableColumn<Pelea, String> peleaDiferenciaColumn;
    @FXML
    private TableColumn<Pelea, String> peleaPartidosColumn;
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
    private ListView<Gallo> gallosSinPeleaList;
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
    private ListView<CotejoGuardado> cotejoGuardadoList;
    @FXML
    private ListView<PeleaGuardada> peleaGuardadaList;
    @FXML
    private ListView<GalloSinPeleaGuardado> galloSinPeleaGuardadoList;
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
        configureSelectionListeners();
        bindListViews();
    }

    private void bindListViews() {
        peleasCotejoTable.setItems(peleasCotejo);
        gallosSinPeleaList.setItems(gallosSinPelea);
        restriccionList.setItems(restricciones);
        cotejoGuardadoList.setItems(cotejosGuardados);
        peleaGuardadaList.setItems(peleasGuardadas);
        galloSinPeleaGuardadoList.setItems(gallosSinPeleaGuardados);
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedPelea) ->
                    mostrarDetallePelea(selectedPelea));
        }
    }

    private void initializeComboBoxes() {
        restriccionPartidoOrigenCombo.setItems(partidos);
        restriccionPartidoDestinoCombo.setItems(partidos);
    }

    private void initializeReglasCotejo() {
        if (modoCotejoComboBox != null) {
            modoCotejoComboBox.setItems(FXCollections.observableArrayList("Aleatorio", "Por rondas"));
            modoCotejoComboBox.getSelectionModel().select(0);
            modoCotejoComboBox.valueProperty().addListener((observable, oldValue, newValue) -> notifyRuleChange());
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

    private void initializePeleasTable() {
        if (peleasCotejoTable == null) {
            return;
        }

        peleasCotejoTable.setPlaceholder(new Label("No hay peleas generadas todavía."));
        peleasCotejoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        peleasCotejoTable.setSortPolicy(table -> false);

        if (peleaNumeroColumn != null) {
            peleaNumeroColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(peleasCotejo.indexOf(cellData.getValue()) + 1));
        }
        if (peleaGallo1Column != null) {
            peleaGallo1Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearAnilloCompacto(cellData.getValue().getGallo1())));
        }
        if (peleaPeso1Column != null) {
            peleaPeso1Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearPesoCompacto(cellData.getValue().getGallo1().getPeso())));
        }
        if (peleaGallo2Column != null) {
            peleaGallo2Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearAnilloCompacto(cellData.getValue().getGallo2())));
        }
        if (peleaPeso2Column != null) {
            peleaPeso2Column.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearPesoCompacto(cellData.getValue().getGallo2().getPeso())));
        }
        if (peleaDiferenciaColumn != null) {
            peleaDiferenciaColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(formatearDiferenciaCompacta(cellData.getValue().getDiferenciaPeso())));
        }
        if (peleaPartidosColumn != null) {
            peleaPartidosColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(
                            abreviarPartido(nombrePartido(cellData.getValue().getGallo1().getPartidoId())) +
                                    " / " +
                                    abreviarPartido(nombrePartido(cellData.getValue().getGallo2().getPartidoId()))));
        }

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

    private void configureSelectionListeners() {
        cotejoGuardadoList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedCotejo) -> {
            if (selectedCotejo != null) {
                verCotejoGuardado();
            }
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
        peleasGuardadas.clear();
        gallosSinPeleaGuardados.clear();
        gallosEvento.clear();
        nombresPartidosEvento.clear();
        cotejoEditableActual = null;
        peleaSeleccionada.set(null);
        if (partidosPrioritariosField != null) {
            partidosPrioritariosField.clear();
        }
        if (reglasCotejoBox != null) {
            reglasCotejoBox.setVisible(false);
            reglasCotejoBox.setManaged(false);
        }
        if (ultimaRondaSoloObligatoriosCheckBox != null) {
            ultimaRondaSoloObligatoriosCheckBox.setSelected(false);
            ultimaRondaSoloObligatoriosCheckBox.setDisable(false);
        }
        if (modoCotejoComboBox != null) {
            modoCotejoComboBox.getSelectionModel().select(0);
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
        if (cotejoGuardadoList != null) {
            cotejoGuardadoList.getSelectionModel().clearSelection();
        }
        if (peleaGuardadaList != null) {
            peleaGuardadaList.getSelectionModel().clearSelection();
        }
        if (galloSinPeleaGuardadoList != null) {
            galloSinPeleaGuardadoList.getSelectionModel().clearSelection();
        }
        if (peleasCotejoTable != null) {
            peleasCotejoTable.getSelectionModel().clearSelection();
        }
        ocultarDetallePelea();
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
        peleasGuardadas.clear();
        gallosSinPeleaGuardados.clear();
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
            ParametrosCotejo parametros = readParametrosCotejo(nombresPartidos, restriccionesPartidos);
            if (parametros == null) {
                return;
            }

            ResultadoCotejo resultado = motorCotejo.cotejar(loadGallosDelEvento(eventoActual), parametros);
            List<Pelea> peleasOrdenadas = ordenadorPeleas.ordenar(
                    resultado.getPeleas(),
                    new ParametrosOrdenamiento(readPartidosPrioritarios(nombresPartidos))
            );
            peleasCotejo.setAll(peleasOrdenadas);
            gallosSinPelea.setAll(resultado.getGallosSinPelea());

            CotejoGuardado cotejoGuardado = guardarCotejo(
                    eventoActual,
                    toleranciaFromParametros(parametros),
                    peleasOrdenadas,
                    resultado.getGallosSinPelea());
            cotejoEditableActual = cotejoGuardado;
            if (peleasCotejoTable != null) {
                peleasCotejoTable.getSelectionModel().clearSelection();
            }
            ocultarDetallePelea();

            loadCotejosGuardados(eventoActual.getId());
            selectCotejoById(cotejoGuardado.getId());

            shellController.setStatus("Cotejo generado: " + resultado.getPeleas().size() +
                    " peleas, " + resultado.getGallosSinPelea().size() + " gallos sin pelea.");
        } catch (Exception e) {
            shellController.showError("Error al cotejar evento", e);
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

    private Set<Long> readPartidosPrioritarios(Map<Long, String> nombresPartidos) {
        Set<Long> partidosPrioritarios = new HashSet<>();
        if (partidosPrioritariosField == null || nombresPartidos == null) {
            return partidosPrioritarios;
        }

        String text = partidosPrioritariosField.getText().trim();
        if (text.isEmpty()) {
            return partidosPrioritarios;
        }

        String[] partes = text.split(",");
        for (String parte : partes) {
            String nombrePrioritario = parte.trim();
            if (nombrePrioritario.isEmpty()) {
                continue;
            }
            for (Map.Entry<Long, String> entry : nombresPartidos.entrySet()) {
                if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(nombrePrioritario)) {
                    partidosPrioritarios.add(entry.getKey());
                }
            }
        }

        return partidosPrioritarios;
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
        if (modoCotejoComboBox != null) {
            modoCotejoComboBox.getSelectionModel().select(
                    evento.getModoCotejo() == ModoCotejo.RONDAS ? 1 : 0);
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
        if (reglasCotejoBox != null) {
            reglasCotejoBox.setVisible(mostrar);
            reglasCotejoBox.setManaged(mostrar);
        }
    }

    private ModoCotejo readModoCotejo() {
        if (modoCotejoComboBox == null) {
            return ModoCotejo.ALEATORIO;
        }
        String seleccionado = modoCotejoComboBox.getSelectionModel().getSelectedItem();
        if ("Por rondas".equalsIgnoreCase(seleccionado)) {
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
                    pelea.getDiferenciaPeso()
            ));
            orden++;
        }
        return peleasGuardadas;
    }

    private List<GalloSinPeleaGuardado> toGallosSinPeleaGuardados(List<Gallo> gallos) {
        List<GalloSinPeleaGuardado> guardados = new ArrayList<>();
        for (Gallo gallo : gallos) {
            guardados.add(new GalloSinPeleaGuardado(null, null, gallo.getId()));
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
            peleaDetallePartido1Label.setText(nombrePartido(pelea.getGallo1().getPartidoId()));
        }
        if (peleaDetallePartido2Label != null) {
            peleaDetallePartido2Label.setText(nombrePartido(pelea.getGallo2().getPartidoId()));
        }
        configurarComboPelea(peleaDetalleGallo1Combo, pelea.getGallo1());
        configurarComboPelea(peleaDetalleGallo2Combo, pelea.getGallo2());
        cargarCamposDetallePelea(pelea);
        actualizarBotonesDetalle(index >= 0);
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
            peleaDetallePartido1Field.setText(nombrePartido(pelea.getGallo1().getPartidoId()));
        }
        if (peleaDetallePartido2Field != null) {
            peleaDetallePartido2Field.setText(nombrePartido(pelea.getGallo2().getPartidoId()));
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
                peleaDetallePartido1Field.setText(nombrePartido(gallo.getPartidoId()));
            }
        } else {
            if (peleaDetalleGallo2PesoField != null) {
                peleaDetalleGallo2PesoField.setText(formatearPesoSinDecimalCero(gallo.getPeso()));
            }
            if (peleaDetalleGallo2AnilloField != null) {
                peleaDetalleGallo2AnilloField.setText(valorTexto(gallo.getAnillo()));
            }
            if (peleaDetallePartido2Field != null) {
                peleaDetallePartido2Field.setText(nombrePartido(gallo.getPartidoId()));
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
            reemplazarPelea(index, crearPelea(gallo1, gallo2));
            persistirCotejoEditable("Pelea actualizada.");
        } catch (Exception e) {
            shellController.showError("Error al guardar los cambios de la pelea", e);
        }
    }

    @FXML
    private void eliminarPeleaSeleccionada() {
        int index = peleasCotejoTable == null ? -1 : peleasCotejoTable.getSelectionModel().getSelectedIndex();
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
            shellController.setStatus(mensajeExito);
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
        return new Pelea(gallo1, gallo2, diferenciaPeso(gallo1, gallo2));
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

    private String abreviarPartido(String partido) {
        if (partido == null || partido.trim().isEmpty()) {
            return "-";
        }
        String limpio = partido.trim();
        return limpio.length() > 18 ? limpio.substring(0, 18) + "..." : limpio;
    }

    private String formatearGallo(Gallo gallo) {
        if (gallo == null) {
            return "-";
        }
        String partido = nombrePartido(gallo.getPartidoId());
        String anillo = gallo.getAnillo() == null || gallo.getAnillo().trim().isEmpty() ? "-" : gallo.getAnillo().trim();
        return gallo.getId() + " | " + formatearPeso(gallo.getPeso()) + " | " + anillo + " | " + partido;
    }

    private String formatearPeso(double peso) {
        return String.format(java.util.Locale.US, "%.1f", peso);
    }

    private TableRow<Pelea> crearFilaArrastrable() {
        TableRow<Pelea> row = new TableRow<Pelea>() {
            @Override
            protected void updateItem(Pelea item, boolean empty) {
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

    private void setDragActive(TableRow<Pelea> row, boolean active) {
        if (row == null) {
            return;
        }
        row.pseudoClassStateChanged(DRAG_ACTIVE, active);
    }

    private void setDropIndicators(TableRow<Pelea> row, boolean top, boolean bottom) {
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

    @FXML
    private void verCotejoGuardado() {
        CotejoGuardado selectedCotejo = cotejoGuardadoList.getSelectionModel().getSelectedItem();
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

            peleasGuardadas.setAll(detalle.getPeleas());
            gallosSinPeleaGuardados.setAll(detalle.getGallosSinPelea());
            shellController.setStatus("Cotejo guardado cargado.");
        } catch (Exception e) {
            shellController.showError("Error al cargar cotejo guardado", e);
        }
    }

    @FXML
    private void exportarPdfCotejoGuardado() {
        CotejoGuardado selectedCotejo = cotejoGuardadoList.getSelectionModel().getSelectedItem();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado para exportar.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar PDF del cotejo");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName("cotejo-" + selectedCotejo.getId() + ".pdf");

            Stage stage = (Stage) cotejoGuardadoList.getScene().getWindow();

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
    private void eliminarCotejoGuardado() {
        CotejoGuardado selectedCotejo = cotejoGuardadoList.getSelectionModel().getSelectedItem();
        if (selectedCotejo == null) {
            shellController.setStatus("Selecciona un cotejo guardado para eliminar.");
            return;
        }

        try {
            cotejoRepository.eliminar(selectedCotejo.getId());
            if (eventoActual != null) {
                loadCotejosGuardados(eventoActual.getId());
            }
            peleasGuardadas.clear();
            gallosSinPeleaGuardados.clear();
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
        if (id == null || cotejoGuardadoList == null) {
            return;
        }
        for (CotejoGuardado cotejo : cotejosGuardados) {
            if (id.equals(cotejo.getId())) {
                cotejoGuardadoList.getSelectionModel().select(cotejo);
                return;
            }
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
