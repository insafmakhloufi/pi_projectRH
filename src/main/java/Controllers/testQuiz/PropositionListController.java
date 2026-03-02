package Controllers.testQuiz;

import Entities.testQuiz.Proposition;
import Entities.testQuiz.Question;
import Entities.testQuiz.Test;
import Services.testQuiz.PropositionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PropositionListController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label questionInfoLabel;

    @FXML
    private TableView<Proposition> propositionTable;

    @FXML
    private TableColumn<Proposition, Integer> colId;

    @FXML
    private TableColumn<Proposition, Integer> colQuestionID;

    @FXML
    private TableColumn<Proposition, String> colContenu;

    @FXML
    private TableColumn<Proposition, Boolean> colEstCorrect;

    private Test selectedTest;
    private Question selectedQuestion;

    private final PropositionService propositionService = new PropositionService();
    private final ObservableList<Proposition> propositionData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuestionID.setCellValueFactory(new PropertyValueFactory<>("questionID"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colEstCorrect.setCellValueFactory(new PropertyValueFactory<>("estCorrect"));

        propositionTable.setItems(propositionData);
    }

    public void setSelectedTest(Test selectedTest) {
        this.selectedTest = selectedTest;
    }

    public void setSelectedQuestion(Question selectedQuestion) {
        this.selectedQuestion = selectedQuestion;
        questionInfoLabel.setText("Propositions de la Question #" + selectedQuestion.getId());
        loadPropositions();
    }

    @FXML
    private void handleAjouter() {
        if (selectedQuestion == null) {
            showError("Contexte", "Aucune question selectionnee.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/proposition_form.fxml"));
            Parent root = loader.load();

            PropositionFormController controller = loader.getController();
            controller.setQuestionId(selectedQuestion.getId());

            Stage stage = new Stage();
            stage.initOwner((Stage) rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Ajouter Proposition");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) {
                if (propositionService.addProposition(controller.getProposition())) {
                    showInfo("Succes", "Proposition ajoutee avec succes.");
                    loadPropositions();
                } else {
                    showError("Erreur", "Impossible d'ajouter la proposition.");
                }
            }
        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir le formulaire Proposition.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        Proposition selected = propositionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Validation", "Selectionnez une proposition e modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/proposition_form.fxml"));
            Parent root = loader.load();

            PropositionFormController controller = loader.getController();
            controller.setQuestionId(selectedQuestion.getId());
            controller.setProposition(selected);

            Stage stage = new Stage();
            stage.initOwner((Stage) rootPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Modifier Proposition");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) {
                if (propositionService.updateProposition(controller.getProposition())) {
                    showInfo("Succes", "Proposition modifiee avec succes.");
                    loadPropositions();
                } else {
                    showError("Erreur", "Impossible de modifier la proposition.");
                }
            }
        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir le formulaire Proposition.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        Proposition selected = propositionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Validation", "Selectionnez une proposition e supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer cette proposition ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (propositionService.deleteProposition(selected.getId())) {
                showInfo("Succes", "Proposition supprimee avec succes.");
                loadPropositions();
            } else {
                showError("Erreur", "Impossible de supprimer la proposition.");
            }
        }
    }

    @FXML
    private void handleRetourQuestions() {
        if (selectedTest == null) {
            return;
        }

        if (selectedTest.getType() != null && selectedTest.getType().equalsIgnoreCase("QCM") && selectedQuestion != null) {
            List<Proposition> props = propositionService.getByQuestionId(selectedQuestion.getId());
            if (props.size() < 2) {
                showWarning("QCM", "Une question QCM doit avoir au moins 2 propositions.");
            }
            if (!propositionService.hasAtLeastOneCorrect(selectedQuestion.getId())) {
                showWarning("QCM", "Une question QCM doit avoir au moins 1 proposition correcte.");
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/question_list.fxml"));
            Parent view = loader.load();

            QuestionListController controller = loader.getController();
            controller.setSelectedTest(selectedTest);

            navigateInParent(view);
        } catch (IOException e) {
            showError("Erreur", "Impossible de retourner vers Questions.");
            e.printStackTrace();
        }
    }

    private void loadPropositions() {
        if (selectedQuestion == null) {
            propositionData.clear();
            return;
        }

        List<Proposition> propositions = propositionService.getByQuestionId(selectedQuestion.getId());
        propositionData.setAll(propositions);

        if (!propositionService.hasAtLeastOneCorrect(selectedQuestion.getId())) {
            showWarning("Avertissement", "Aucune proposition correcte pour cette question (non bloquant).");
        }
    }

    private void navigateInParent(Parent view) {
        if (rootPane.getParent() instanceof StackPane parent) {
            parent.getChildren().setAll(view);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
