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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PeleasController {
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
    private final ObservableList<RestriccionPartido> restricciones = FXCollections.observableArrayList();
    private final ObservableList<CotejoGuardado> cotejosGuardados = FXCollections.observableArrayList();
    private final ObservableList<PeleaGuardada> peleasGuardadas = FXCollections.observableArrayList();
    private final ObservableList<GalloSinPeleaGuardado> gallosSinPeleaGuardados = FXCollections.observableArrayList();

    @FXML
    private TextField toleranciaField;
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
    private TableColumn<Pelea, String> gallo1Column;
    @FXML
    private TableColumn<Pelea, String> partido1Column;
    @FXML
    private TableColumn<Pelea, String> gallo2Column;
    @FXML
    private TableColumn<Pelea, String> partido2Column;
    @FXML
    private TableColumn<Pelea, String> diferenciaColumn;
    @FXML
    private ListView<Gallo> gallosSinPeleaList;
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

    public void setShellController(ShellController shellController) {
        this.shellController = shellController;
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
        initializePeleasTable();
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
        if (peleasCotejoTable != null) {
            peleasCotejoTable.setPlaceholder(new Label("No hay peleas generadas todavía."));
            peleasCotejoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (gallo1Column != null) {
            gallo1Column.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(data.getValue().getGallo1().getNombre()));
        }
        if (partido1Column != null) {
            partido1Column.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(nombrePartido(data.getValue().getGallo1().getPartidoId())));
        }
        if (gallo2Column != null) {
            gallo2Column.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(data.getValue().getGallo2().getNombre()));
        }
        if (partido2Column != null) {
            partido2Column.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(nombrePartido(data.getValue().getGallo2().getPartidoId())));
        }
        if (diferenciaColumn != null) {
            diferenciaColumn.setCellValueFactory(data ->
                    new ReadOnlyStringWrapper(formatearDiferencia(data.getValue().getDiferenciaPeso())));
        }
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
        if (toleranciaField != null) {
            toleranciaField.clear();
        }
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
    }

    private PartidoRepository getPartidoRepository() {
        return partidoRepository;
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
            Map<Long, String> nombresPartidos = loadNombresPartidosDelEvento(eventoActual);
            List<RestriccionPartido> restriccionesPartidos =
                    restriccionPartidoRepository.listarPorEvento(eventoActual.getId());
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

    private Map<Long, String> loadNombresPartidosDelEvento(Evento evento) throws Exception {
        Map<Long, String> nombresPartidos = new HashMap<>();
        List<Partido> partidosDelEvento = partidoRepository.listarPorEvento(evento.getId());
        for (Partido partido : partidosDelEvento) {
            nombresPartidos.put(partido.getId(), partido.getNombre());
        }
        return nombresPartidos;
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
        if (toleranciaField == null) {
            shellController.setStatus("La tolerancia es obligatoria.");
            return null;
        }

        String toleranciaText = toleranciaField.getText().trim();
        if (toleranciaText.isEmpty()) {
            shellController.setStatus("La tolerancia es obligatoria.");
            return null;
        }

        double tolerancia;
        try {
            tolerancia = Double.parseDouble(toleranciaText);
        } catch (NumberFormatException e) {
            shellController.setStatus("La tolerancia debe ser numerica.");
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
