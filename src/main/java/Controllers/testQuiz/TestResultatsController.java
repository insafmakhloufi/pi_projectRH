package Controllers.testQuiz;

import Entities.testQuiz.Test;
import Entities.testQuiz.TestResultat;
import Services.testQuiz.QuizPdfExportService;
import Services.testQuiz.TestResultatService;
import Utils.front.FrontQuizResult;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class TestResultatsController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private TableView<TestResultat> resultatsTable;
    @FXML private TableColumn<TestResultat, String> testCol;
    @FXML private TableColumn<TestResultat, String> candidatCol;
    @FXML private TableColumn<TestResultat, String> scoreCol;
    @FXML private TableColumn<TestResultat, String> percentCol;
    @FXML private TableColumn<TestResultat, String> mentionCol;
    @FXML private TableColumn<TestResultat, Number> correctCol;
    @FXML private TableColumn<TestResultat, Number> wrongCol;
    @FXML private TableColumn<TestResultat, String> tempsCol;
    @FXML private TableColumn<TestResultat, String> dateCol;
    @FXML private Button exportPdfBtn;

    private final TestResultatService service = new TestResultatService();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initColumns();
        if (exportPdfBtn != null) {
            exportPdfBtn.setDisable(true);
        }
        if (resultatsTable != null) {
            resultatsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (exportPdfBtn != null) exportPdfBtn.setDisable(n == null);
            });
        }
        loadResultats();
    }

    @FXML
    private void handleRefresh() {
        loadResultats();
    }

    @FXML
    private void handleExportPdf() {
        TestResultat selected = resultatsTable == null ? null : resultatsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Test test = new Test();
        test.setId(selected.getTestId());
        test.setTitre(selected.getTestTitre());
        test.setType("");

        FrontQuizResult result = new FrontQuizResult(
                test,
                selected.getScore(),
                Math.max(1, selected.getMaxScore()),
                selected.getCorrect(),
                selected.getWrong(),
                selected.getUnanswered(),
                selected.getTimeSpentSec(),
                List.of(),
                new Integer[0],
                new String[0],
                selected.getDatePassage()
        );
        QuizPdfExportService.exportWithChooser(result, resultatsTable.getScene().getWindow());
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/test_list.fxml"));
            Parent view = loader.load();
            navigateInParent(view);
        } catch (IOException e) {
            showError("Erreur", "Impossible de retourner vers la liste des tests.");
        }
    }

    private void loadResultats() {
        List<TestResultat> resultats = service.getAllResultats();
        if (resultatsTable != null) {
            resultatsTable.setItems(FXCollections.observableArrayList(resultats));
        }
    }

    private void initColumns() {
        testCol.setCellValueFactory(v -> new SimpleStringProperty(safe(v.getValue().getTestTitre())));
        candidatCol.setCellValueFactory(v -> new SimpleStringProperty(safe(v.getValue().getCandidatNom())));
        scoreCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getScore() + " / " + v.getValue().getMaxScore()));
        percentCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getPercent() + "%"));
        mentionCol.setCellValueFactory(v -> new SimpleStringProperty(safe(v.getValue().getMention())));
        correctCol.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getCorrect()));
        wrongCol.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getWrong()));
        tempsCol.setCellValueFactory(v -> new SimpleStringProperty(formatTime(v.getValue().getTimeSpentSec())));
        dateCol.setCellValueFactory(v -> new SimpleStringProperty(
                v.getValue().getDatePassage() == null ? "" : v.getValue().getDatePassage().format(DT)
        ));

        mentionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String val = item.trim().toLowerCase();
                if (val.contains("excellent")) {
                    setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                } else if (val.contains("good")) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            }
        });
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void navigateInParent(Parent view) {
        if (rootPane != null && rootPane.getParent() instanceof StackPane parent) {
            parent.getChildren().setAll(view);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
