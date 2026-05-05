package com.cotejador.app.desktop;

import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.ModoCotejo;
import com.cotejador.app.data.sqlite.CotejoRepository;
import com.cotejador.app.data.sqlite.EventoRepository;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import com.cotejador.app.data.sqlite.PdfCotejoService;
import com.cotejador.app.data.sqlite.RestriccionPartidoRepository;
import com.cotejador.app.data.sqlite.SQLiteConnectionFactory;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.util.Duration;

public class ShellController {

    private enum Theme {
        DARK,
        LIGHT
    }

    private static final double DRAWER_WIDTH = 250.0;
    private static final double EVENTOS_WINDOW_WIDTH = 800.0;
    private static final double EVENTOS_WINDOW_HEIGHT = 680.0;
    private static final double CONTENT_MIN_WINDOW_WIDTH = 1240.0;
    private static final double CONTENT_MIN_WINDOW_HEIGHT = 820.0;

    private enum WindowMode {
        EVENTOS,
        REGISTRO,
        PELEAS
    }

    @FXML
    private BorderPane shellRoot;

    @FXML
    private Label statusLabel;

    @FXML
    private Button themeToggleButton;

    @FXML
    private Button menuButton;

    @FXML
    private javafx.scene.layout.HBox topNavBar;

    @FXML
    private Button topEventosButton;

    @FXML
    private Button topRegistroButton;

    @FXML
    private Button topPeleasButton;

    @FXML
    private StackPane contentStack;

    @FXML
    private AnchorPane eventosHost;

    @FXML
    private AnchorPane registroHost;

    @FXML
    private AnchorPane peleasHost;

    @FXML
    private javafx.scene.layout.VBox drawerPanel;

    @FXML
    private Region drawerBackdrop;

    @FXML
    private javafx.scene.layout.HBox eventInfoBar;

    @FXML
    private Label eventNameLabel;

    @FXML
    private Label eventDateLabel;

    @FXML
    private Label eventGallosLabel;

    @FXML
    private Label eventPartidosLabel;
    @FXML
    private Label eventTotalPartidosLabel;

    @FXML
    private Label eventModeLabel;

    @FXML
    private Button drawerEventosButton;

    @FXML
    private Button drawerRegistroButton;

    @FXML
    private Button drawerPeleasButton;

    @FXML
    private Button drawerCloseButton;

    private EventosController eventosController;
    private RegistroController registroController;
    private PeleasController peleasController;

    private EventoRepository eventoRepository;
    private PartidoRepository partidoRepository;
    private GalloRepository galloRepository;
    private CotejoRepository cotejoRepository;
    private PdfCotejoService pdfCotejoService;
    private RestriccionPartidoRepository restriccionPartidoRepository;
    private Theme currentTheme = Theme.DARK;
    private Evento currentEvento;
    private boolean drawerOpen;
    private WindowMode currentWindowMode = WindowMode.EVENTOS;
    private boolean maximizeRegistroOnOpen;
    private javafx.stage.Stage primaryStage;

    public void initialize() {
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory();

        eventoRepository = new EventoRepository(connectionFactory);
        partidoRepository = new PartidoRepository(connectionFactory);
        galloRepository = new GalloRepository(connectionFactory);
        cotejoRepository = new CotejoRepository(connectionFactory);
        pdfCotejoService = new PdfCotejoService(cotejoRepository, eventoRepository, partidoRepository, galloRepository);
        restriccionPartidoRepository = new RestriccionPartidoRepository(connectionFactory);

        cargarVistasHijas();
        mostrarVistaEventos();
        loadEventos();
        setStatus("Selecciona o crea un evento para continuar.");
        refreshThemeToggleText();
        setDrawerVisible(false);
        updateTopNavigationVisibility(false);
        updateEventInfo(null, ModoCotejo.ALEATORIO);
        applyWindowMode();
    }

    public void setPrimaryStage(javafx.stage.Stage primaryStage) {
        this.primaryStage = primaryStage;
        applyWindowMode();
    }

    public boolean isDarkThemeActive() {
        return currentTheme == Theme.DARK;
    }

    private void cargarVistasHijas() {
        try {
            FXMLLoader eventosLoader = new FXMLLoader(getClass().getResource("/EventosView.fxml"));
            Parent eventosView = eventosLoader.load();
            eventosController = eventosLoader.getController();
            eventosController.setShellController(this);
            eventosController.setRepositories(eventoRepository);
            attachToHost(eventosView, eventosHost);

            FXMLLoader registroLoader = new FXMLLoader(getClass().getResource("/RegistroView.fxml"));
            Parent registroView = registroLoader.load();
            registroController = registroLoader.getController();
            registroController.setShellController(this);
            registroController.setRepositories(partidoRepository, galloRepository);
            attachToHost(registroView, registroHost);

            FXMLLoader peleasLoader = new FXMLLoader(getClass().getResource("/PeleasView.fxml"));
            Parent peleasView = peleasLoader.load();
            peleasController = peleasLoader.getController();
            peleasController.setShellController(this);
            peleasController.setRepositories(eventoRepository, partidoRepository, galloRepository,
                    cotejoRepository, pdfCotejoService, restriccionPartidoRepository);
            attachToHost(peleasView, peleasHost);
        } catch (Exception e) {
            setStatus("Error al cargar vistas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void attachToHost(Parent view, AnchorPane host) {
        host.getChildren().setAll(view);
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
    }

    private void loadEventos() {
        try {
            eventosController.loadEventos();
        } catch (Exception e) {
            setStatus("Error al cargar eventos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEventoSeleccionado(Evento evento) {
        currentEvento = evento;
        if (evento != null) {
            try {
                maximizeRegistroOnOpen = true;
                eventoInfoVisible(true);
                drawerOpen = false;
                setDrawerVisible(false);
                updateTopNavigationVisibility(true);
                if (registroController != null) {
                    registroController.onEventoCambio(evento);
                }
                ModoCotejo modoCotejo = ModoCotejo.ALEATORIO;
                if (peleasController != null) {
                    peleasController.onEventoCambio(evento);
                    ModoCotejo modoSeleccionado = peleasController.getModoCotejoSeleccionado();
                    if (modoSeleccionado != null) {
                        modoCotejo = modoSeleccionado;
                    }
                }
                updateEventInfo(evento, modoCotejo);
                mostrarVistaRegistro();
            } catch (Exception e) {
                setStatus("Error al actualizar vistas: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (registroController != null) {
                registroController.limpiar();
            }
            if (peleasController != null) {
                peleasController.limpiar();
            }
            updateEventInfo(null, ModoCotejo.ALEATORIO);
            setDrawerVisible(false);
            updateTopNavigationVisibility(false);
            mostrarVistaEventos();
        }
    }

    public void actualizarInfoEvento(Evento evento, ModoCotejo modoCotejo) {
        if (evento == null) {
            updateEventInfo(null, ModoCotejo.ALEATORIO);
            return;
        }
        updateEventInfo(evento, modoCotejo);
    }

    public void refrescarPartidosEnPeleas() {
        if (peleasController != null) {
            peleasController.refrescarPartidosEventoActual();
        }
    }

    public void refrescarInfoEventoActual() {
        if (currentEvento == null) {
            updateEventInfo(null, ModoCotejo.ALEATORIO);
            return;
        }
        ModoCotejo modoCotejo = ModoCotejo.ALEATORIO;
        if (peleasController != null) {
            ModoCotejo modoSeleccionado = peleasController.getModoCotejoSeleccionado();
            if (modoSeleccionado != null) {
                modoCotejo = modoSeleccionado;
            }
        }
        updateEventInfo(currentEvento, modoCotejo);
    }

    private void updateEventInfo(Evento evento, ModoCotejo modoCotejo) {
        if (eventInfoBar == null) {
            return;
        }

        if (evento == null) {
            eventInfoBar.setVisible(false);
            eventInfoBar.setManaged(false);
            eventNameLabel.setText("-");
            eventDateLabel.setText("-");
            eventGallosLabel.setText("-");
            eventPartidosLabel.setText("-");
            eventTotalPartidosLabel.setText("-");
            eventModeLabel.setText("-");
            return;
        }

        eventInfoBar.setVisible(true);
        eventInfoBar.setManaged(true);
        eventNameLabel.setText(safeText(evento.getNombre()));
        eventDateLabel.setText(safeText(evento.getFecha()));
        eventGallosLabel.setText(String.valueOf(evento.getGallosPorPartido()));
        eventTotalPartidosLabel.setText(obtenerTotalPartidos(evento));
        eventPartidosLabel.setText(obtenerTotalEntradas(evento));
        eventModeLabel.setText(modoCotejo == ModoCotejo.RONDAS ? "Por rondas" : "Aleatorio");
    }

    private String obtenerTotalPartidos(Evento evento) {
        if (evento == null || evento.getId() == null || partidoRepository == null) {
            return "-";
        }
        try {
            return String.valueOf(partidoRepository.listarPorEvento(evento.getId()).size());
        } catch (Exception e) {
            return "-";
        }
    }

    private String obtenerTotalEntradas(Evento evento) {
        if (evento == null || evento.getId() == null || partidoRepository == null || galloRepository == null) {
            return "-";
        }
        int gallosPorPartido = evento.getGallosPorPartido();
        if (gallosPorPartido <= 0) {
            return "-";
        }
        try {
            int totalEntradas = 0;
            for (com.cotejador.app.core.model.Partido partido : partidoRepository.listarPorEvento(evento.getId())) {
                int totalGallosPartido = galloRepository.listarPorPartido(partido.getId()).size();
                if (totalGallosPartido <= 0) {
                    continue;
                }
                totalEntradas += (totalGallosPartido + gallosPorPartido - 1) / gallosPorPartido;
            }
            return String.valueOf(totalEntradas);
        } catch (Exception e) {
            return "-";
        }
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    public void mostrarVistaEventos() {
        currentWindowMode = WindowMode.EVENTOS;
        setHostVisibility(true, false, false);
        if (menuButton != null) {
            menuButton.setDisable(true);
        }
        setActiveNavigationView(topEventosButton);
        applyWindowMode();
    }

    public void mostrarVistaRegistro() {
        if (currentEvento == null) {
            mostrarVistaEventos();
            return;
        }
        currentWindowMode = WindowMode.REGISTRO;
        setHostVisibility(false, true, false);
        setActiveNavigationView(topRegistroButton);
        applyWindowMode();
        if (maximizeRegistroOnOpen) {
            maximizeRegistroOnOpen = false;
            maximizeContentWindow();
        }
    }

    public void mostrarVistaPeleas() {
        if (currentEvento == null) {
            mostrarVistaEventos();
            return;
        }
        currentWindowMode = WindowMode.PELEAS;
        setHostVisibility(false, false, true);
        setActiveNavigationView(topPeleasButton);
        applyWindowMode();
    }

    @FXML
    private void toggleDrawer() {
        if (currentEvento == null || drawerPanel == null) {
            return;
        }
        drawerOpen = !drawerOpen;
        moveDrawer(drawerOpen);
    }

    @FXML
    private void mostrarRegistro() {
        mostrarVistaRegistro();
    }

    @FXML
    private void mostrarPeleas() {
        mostrarVistaPeleas();
    }

    @FXML
    private void volverAEventos() {
        if (currentEvento == null) {
            mostrarVistaEventos();
            return;
        }
        onEventoSeleccionado(null);
    }

    public void setStatus(String mensaje) {
        statusLabel.setText(mensaje);
    }

    public void showError(String mensaje, Exception e) {
        e.printStackTrace();
        setStatus(mensaje + ": " + e.getMessage());
    }

    @FXML
    private void toggleTheme() {
        currentTheme = currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK;
        applyTheme();
        refreshThemeToggleText();
    }

    private void applyTheme() {
        if (shellRoot == null) {
            return;
        }

        Scene scene = shellRoot.getScene();
        if (scene == null) {
            return;
        }

        String themeStylesheet = currentTheme == Theme.DARK
                ? getClass().getResource("/theme-dark.css").toExternalForm()
                : getClass().getResource("/theme-light.css").toExternalForm();

        scene.getStylesheets().removeIf(stylesheet ->
                stylesheet.endsWith("/theme-dark.css") || stylesheet.endsWith("/theme-light.css"));
        scene.getStylesheets().add(themeStylesheet);

        if (peleasController != null) {
            peleasController.aplicarTemaDetallePelea();
        }
    }

    private void refreshThemeToggleText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(currentTheme == Theme.DARK ? "Tema claro" : "Tema oscuro");
        }
    }

    private void setHostVisibility(boolean eventosVisible, boolean registroVisible, boolean peleasVisible) {
        setHostVisible(eventosHost, eventosVisible);
        setHostVisible(registroHost, registroVisible);
        setHostVisible(peleasHost, peleasVisible);
    }

    private void setHostVisible(AnchorPane host, boolean visible) {
        if (host == null) {
            return;
        }
        host.setVisible(visible);
        host.setManaged(visible);
    }

    private void eventoInfoVisible(boolean visible) {
        if (eventInfoBar == null) {
            return;
        }
        eventInfoBar.setVisible(visible);
        eventInfoBar.setManaged(visible);
    }

    private void setDrawerVisible(boolean visible) {
        if (drawerPanel == null || shellRoot == null) {
            return;
        }
        if (visible) {
            shellRoot.setLeft(drawerPanel);
            drawerPanel.setVisible(true);
            drawerPanel.setManaged(true);
            drawerPanel.setTranslateX(0.0);
        } else {
            drawerPanel.setVisible(false);
            drawerPanel.setManaged(false);
            shellRoot.setLeft(null);
        }
        if (drawerBackdrop != null) {
            drawerBackdrop.setVisible(visible);
            drawerBackdrop.setManaged(visible);
        }
        if (!visible) {
            drawerPanel.setTranslateX(0.0);
        }
    }

    private void moveDrawer(boolean open) {
        if (drawerPanel == null || shellRoot == null) {
            return;
        }
        if (open) {
            shellRoot.setLeft(drawerPanel);
            drawerPanel.setVisible(true);
            drawerPanel.setManaged(true);
            drawerPanel.setTranslateX(-DRAWER_WIDTH);
            if (drawerBackdrop != null) {
                drawerBackdrop.setVisible(true);
                drawerBackdrop.setManaged(true);
            }
            TranslateTransition transition = new TranslateTransition(Duration.millis(180), drawerPanel);
            transition.setToX(0.0);
            transition.play();
            return;
        }

        TranslateTransition transition = new TranslateTransition(Duration.millis(180), drawerPanel);
        transition.setToX(-DRAWER_WIDTH);
        transition.setOnFinished(event -> {
            drawerPanel.setVisible(false);
            drawerPanel.setManaged(false);
            shellRoot.setLeft(null);
            if (drawerBackdrop != null) {
                drawerBackdrop.setVisible(false);
                drawerBackdrop.setManaged(false);
            }
            drawerPanel.setTranslateX(0.0);
        });
        transition.play();
    }

    private void setActiveNavigationView(Button activeButton) {
        setTopNavButtonActive(topEventosButton, activeButton != null && activeButton == topEventosButton);
        setTopNavButtonActive(topRegistroButton, activeButton != null && activeButton == topRegistroButton);
        setTopNavButtonActive(topPeleasButton, activeButton != null && activeButton == topPeleasButton);
        setDrawerButtonActive(drawerEventosButton, false);
        setDrawerButtonActive(drawerRegistroButton, false);
        setDrawerButtonActive(drawerPeleasButton, false);
    }

    private void setDrawerButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("active-view")) {
                button.getStyleClass().add("active-view");
            }
        } else {
            button.getStyleClass().remove("active-view");
        }
    }

    private void setTopNavButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("active-view")) {
                button.getStyleClass().add("active-view");
            }
        } else {
            button.getStyleClass().remove("active-view");
        }
    }

    private void updateTopNavigationVisibility(boolean visible) {
        if (topNavBar == null) {
            return;
        }
        topNavBar.setVisible(visible);
        topNavBar.setManaged(visible);
    }

    private void applyWindowMode() {
        if (primaryStage == null) {
            return;
        }

        Platform.runLater(() -> {
            if (primaryStage == null) {
                return;
            }

            if (currentWindowMode == WindowMode.EVENTOS) {
                primaryStage.setMaximized(false);
                primaryStage.setResizable(false);
                primaryStage.setMinWidth(0.0);
                primaryStage.setMinHeight(0.0);
                primaryStage.setWidth(EVENTOS_WINDOW_WIDTH);
                primaryStage.setHeight(EVENTOS_WINDOW_HEIGHT);
                primaryStage.centerOnScreen();
                return;
            }

            if (currentWindowMode == WindowMode.REGISTRO || currentWindowMode == WindowMode.PELEAS) {
                primaryStage.setResizable(true);
                primaryStage.setMinWidth(CONTENT_MIN_WINDOW_WIDTH);
                primaryStage.setMinHeight(CONTENT_MIN_WINDOW_HEIGHT);
                return;
            }

            primaryStage.setMinWidth(0.0);
            primaryStage.setMinHeight(0.0);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
        });
    }

    private void maximizeContentWindow() {
        if (primaryStage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.setResizable(true);
                primaryStage.setMinWidth(CONTENT_MIN_WINDOW_WIDTH);
                primaryStage.setMinHeight(CONTENT_MIN_WINDOW_HEIGHT);
                primaryStage.setWidth(Math.max(CONTENT_MIN_WINDOW_WIDTH, Screen.getPrimary().getVisualBounds().getWidth()));
                primaryStage.setHeight(Math.max(CONTENT_MIN_WINDOW_HEIGHT, Screen.getPrimary().getVisualBounds().getHeight()));
                primaryStage.setX(Screen.getPrimary().getVisualBounds().getMinX());
                primaryStage.setY(Screen.getPrimary().getVisualBounds().getMinY());
                primaryStage.setMaximized(true);
            }
        });
    }
}
