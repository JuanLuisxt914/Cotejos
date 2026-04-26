package com.cotejador.app.desktop;

import com.cotejador.app.core.model.Evento;
import com.cotejador.app.core.model.Gallo;
import com.cotejador.app.core.model.Partido;
import com.cotejador.app.data.sqlite.GalloRepository;
import com.cotejador.app.data.sqlite.PartidoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class RegistroController {

    @FXML
    private TextField partidoNombreField;

    @FXML
    private TextField galloPesoField;

    @FXML
    private TextField galloAnilloField;

    @FXML
    private CheckBox galloObligatorioCheckBox;

    @FXML
    private ListView<Partido> partidoList;

    @FXML
    private ListView<Gallo> galloList;

    @FXML
    private Button agregarPartidoButton;

    @FXML
    private Button editarPartidoButton;

    @FXML
    private Button eliminarPartidoButton;

    @FXML
    private Button agregarGalloButton;

    @FXML
    private Button editarGalloButton;

    @FXML
    private Button eliminarGalloButton;

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
        partidoList.setItems(partidos);
        galloList.setItems(gallos);
        galloList.setCellFactory(listView -> new ListCell<Gallo>() {
            @Override
            protected void updateItem(Gallo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(formatGalloListItem(item, getIndex()));
            }
        });
        configureSelectionListeners();
    }

    @FXML
    public void initialize() {
        partidoNombreField.setPromptText("Nombre del partido");
        galloPesoField.setPromptText("Peso en gramos");
        galloAnilloField.setPromptText("Anillo");
        updateGalloObligatorioVisibility(null);
    }

    private void configureSelectionListeners() {
        partidoList.getSelectionModel().selectedItemProperty().addListener((observable, oldPartido, selectedPartido) -> {
            if (selectedPartido != null) {
                partidoNombreField.setText(selectedPartido.getNombre());
                try {
                    gallos.setAll(galloRepository.listarPorPartido(selectedPartido.getId()));
                } catch (Exception e) {
                    shellController.showError("Error al cargar gallos", e);
                }
            } else {
                partidoNombreField.clear();
                gallos.clear();
            }
        });

        galloList.getSelectionModel().selectedItemProperty().addListener((observable, oldGallo, selectedGallo) -> {
            if (selectedGallo != null) {
                galloPesoField.setText(String.valueOf(selectedGallo.getPeso()));
                galloAnilloField.setText(selectedGallo.getAnillo() == null ? "" : selectedGallo.getAnillo());
                galloObligatorioCheckBox.setSelected(selectedGallo.isObligatorio());
            } else {
                clearGalloFields();
            }
        });
    }

    public void onEventoCambio(Evento evento) throws Exception {
        this.eventoActual = evento;
        if (evento != null) {
            partidos.setAll(partidoRepository.listarPorEvento(evento.getId()));
        } else {
            partidos.clear();
            gallos.clear();
        }
        updateGalloObligatorioVisibility(evento);
        clearFields();
    }

    public void limpiar() {
        partidos.clear();
        gallos.clear();
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

            Partido partido = new Partido(null, nombre, eventoActual.getId());
            partidoRepository.insertar(partido);
            loadPartidos(partido.getId());
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
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido para editar.");
                return;
            }

            String nombre = partidoNombreField.getText().trim();
            if (nombre.isEmpty()) {
                shellController.setStatus("El nombre del partido es obligatorio.");
                return;
            }

            Partido partido = new Partido(selectedPartido.getId(), nombre, eventoActual.getId());
            partidoRepository.actualizar(partido);
            loadPartidos(partido.getId());
            clearPartidoFields();
            shellController.setStatus("Partido actualizado.");
        } catch (Exception e) {
            shellController.showError("Error al editar partido", e);
        }
    }

    @FXML
    public void eliminarPartido() {
        try {
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            if (selectedPartido == null) {
                shellController.setStatus("Selecciona un partido para eliminar.");
                return;
            }

            partidoRepository.eliminar(selectedPartido.getId());
            loadPartidos(null);
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
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
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
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloList.getSelectionModel().getSelectedItem();
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
            Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
            Gallo selectedGallo = galloList.getSelectionModel().getSelectedItem();
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
        boolean obligatorio = galloObligatorioCheckBox.isSelected();

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

        return new Gallo(id, peso, anillo, partidoId, obligatorio);
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
            partidoList.getSelectionModel().clearSelection();
            return;
        }
        for (Partido partido : partidos) {
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
        for (Gallo gallo : gallos) {
            if (id.equals(gallo.getId())) {
                galloList.getSelectionModel().select(gallo);
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
    }

    private void clearGalloFields() {
        galloPesoField.clear();
        galloAnilloField.clear();
        galloObligatorioCheckBox.setSelected(false);
    }

    private void updateGalloObligatorioVisibility(Evento evento) {
        boolean mostrar = evento != null && evento.getGallosPorPartido() > 1;
        if (galloObligatorioCheckBox != null) {
            galloObligatorioCheckBox.setVisible(mostrar);
            galloObligatorioCheckBox.setManaged(mostrar);
            if (!mostrar) {
                galloObligatorioCheckBox.setSelected(false);
            }
        }
    }

    private void clearGalloSelection() {
        if (galloList != null) {
            galloList.getSelectionModel().clearSelection();
        }
    }

    private String formatGalloListItem(Gallo gallo, int index) {
        String partidoNombre = obtenerNombrePartidoSeleccionado(gallo);
        int gallosPorPartido = eventoActual == null ? 1 : eventoActual.getGallosPorPartido();
        if (gallosPorPartido <= 0) {
            gallosPorPartido = 1;
        }
        String entrada = "E" + ((index / gallosPorPartido) + 1);
        String peso = formatNumero(gallo.getPeso());
        String anillo = gallo.getAnillo() == null || gallo.getAnillo().trim().isEmpty()
                ? "-"
                : gallo.getAnillo().trim();

        StringBuilder builder = new StringBuilder();
        builder.append(entrada)
                .append(" ")
                .append(partidoNombre)
                .append(" | ")
                .append(peso)
                .append(" | ")
                .append(anillo);

        if (gallo.isObligatorio()) {
            builder.append(" | obligatorio");
        }

        return builder.toString();
    }

    private String obtenerNombrePartidoSeleccionado(Gallo gallo) {
        Partido selectedPartido = partidoList.getSelectionModel().getSelectedItem();
        if (selectedPartido != null && selectedPartido.getId() != null
                && selectedPartido.getId().equals(gallo.getPartidoId())) {
            return safeText(selectedPartido.getNombre());
        }
        return "PARTIDO " + gallo.getPartidoId();
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
}
