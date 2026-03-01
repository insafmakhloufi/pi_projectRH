package Controllers.testQuiz;

import Entities.testQuiz.CandidatItem;
import Entities.testQuiz.Test;
import Services.testQuiz.AiTestGeneratorService;
import Services.testQuiz.TestService;
import Utils.UiTranslator;
import javafx.application.Platform;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TestListController implements Initializable {

    private enum FormMode { ADD, EDIT }

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox leftBlock;
    @FXML
    private VBox rightBlock;
    @FXML
    private Label formModeLabel;
    @FXML
    private ComboBox<CandidatItem> formCandidatCombo;
    @FXML
    private Label candidateValidationLabel;
    @FXML
    private TextField formTitreField;
    @FXML
    private ComboBox<String> formTypeCombo;
    @FXML
    private Label titreValidationLabel;
    @FXML
    private Label typeValidationLabel;
    @FXML
    private TextField formScoreMaxField;
    @FXML
    private Label scoreValidationLabel;
    @FXML
    private TextField formDurationSecondsField;
    @FXML
    private Label durationValidationLabel;
    @FXML
    private TextField formTotalPointsField;
    @FXML
    private Label totalPointsValidationLabel;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private TextField searchTitreField;
    @FXML
    private TextField searchTypeField;
    @FXML
    private TextField searchCandidatField;
    @FXML
    private FlowPane testsFlow;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private Button langFrButton;
    @FXML
    private Button langEnButton;
    @FXML
    private Button langArButton;

    private final TestService testService = new TestService();
    private final AiTestGeneratorService aiService = new AiTestGeneratorService();
    private final UiTranslator uiTranslator = new UiTranslator();

    private FormMode formMode = FormMode.ADD;
    private Integer editingTestId = null;
    private Test selectedTest = null;
    private static final String[] TEST_TYPES = new String[]{"QCM", "LOGIQUE", "CODING"};
    private static final String[] DIFFICULTIES = new String[]{"Facile", "Moyen", "Difficile"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initCombos();
        setupLiveValidation();
        searchTitreField.textProperty().addListener((obs, oldVal, newVal) -> loadTests());
        searchTypeField.textProperty().addListener((obs, oldVal, newVal) -> loadTests());
        searchCandidatField.textProperty().addListener((obs, oldVal, newVal) -> loadTests());

        clearForm();
        validateFormAndToggleActions();
        loadTests();
    }

    @FXML
    private void handleAjouter() {
        try {
            Test test = buildTestFromForm();
            if (testService.addTest(test)) {
                showInfo("Succes", "Test ajoute avec succes.");
                selectedTest = null;
                clearForm();
                setAddMode();
                loadTests();
            } else {
                showError("Erreur", "Impossible d'ajouter le test.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (selectedTest == null) {
            showError("Validation", "Selectionnez un test a modifier.");
            return;
        }
        try {
            Test form = buildTestFromForm();
            Test test = new Test(selectedTest.getId(), form.getCandidatID(), form.getTitre(), form.getType(),
                    form.getScoreMax(), form.getDurationSeconds(), form.getTotalPoints());
            if (testService.updateTest(test)) {
                showInfo("Succes", "Test modifie avec succes.");
                selectedTest = null;
                clearForm();
                setAddMode();
                loadTests();
            } else {
                showError("Erreur", "Impossible de modifier le test.");
            }
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void handleAnnulerForm() {
        selectedTest = null;
        clearForm();
        setAddMode();
        refreshCardsSelection();
    }

    @FXML
    private void handleSupprimer() {
        if (selectedTest == null) {
            showError("Validation", "Selectionnez un test a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce test ? Les questions et propositions liees seront supprimees (CASCADE).");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (testService.deleteTest(selectedTest.getId())) {
                showInfo("Succes", "Test supprime avec succes.");
                selectedTest = null;
                clearForm();
                setAddMode();
                loadTests();
            } else {
                showError("Erreur", "Impossible de supprimer le test.");
            }
        }
    }

    @FXML
    private void handleRafraichir() {
        loadTests();
    }

    @FXML
    private void handleQuestions() {
        if (selectedTest == null) {
            showError("Validation", "Selectionnez un test pour afficher ses questions.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/question_list.fxml"));
            Parent view = loader.load();

            QuestionListController controller = loader.getController();
            controller.setSelectedTest(selectedTest);

            navigateInParent(view);
        } catch (IOException e) {
            showError("Erreur", "Impossible de charger la liste des questions.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGenererIA() {
        Stage owner = (Stage) rootPane.getScene().getWindow();
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Generer un Test (IA)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("tests-form-content");

        ComboBox<CandidatItem> candidatCombo = new ComboBox<>();
        candidatCombo.getItems().setAll(testService.getAllCandidats());
        candidatCombo.setPromptText("Candidat (Nom Prenom)");
        candidatCombo.getStyleClass().add("input");

        Label candidatErr = new Label();
        candidatErr.getStyleClass().add("validation-text");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().setAll(TEST_TYPES);
        typeCombo.setPromptText("Type");
        typeCombo.getStyleClass().add("input");

        Label typeErr = new Label();
        typeErr.getStyleClass().add("validation-text");

        Spinner<Integer> nbSpinner = new Spinner<>();
        nbSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 30, 10, 1));
        nbSpinner.setEditable(true);
        nbSpinner.getStyleClass().add("input");

        Label nbErr = new Label();
        nbErr.getStyleClass().add("validation-text");

        ComboBox<String> difficulteCombo = new ComboBox<>();
        difficulteCombo.getItems().setAll(DIFFICULTIES);
        difficulteCombo.setPromptText("Difficulte");
        difficulteCombo.getStyleClass().add("input");

        Label diffErr = new Label();
        diffErr.getStyleClass().add("validation-text");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description libre du test a generer...");
        descriptionArea.setPrefRowCount(6);
        descriptionArea.getStyleClass().add("input");

        Label descErr = new Label();
        descErr.getStyleClass().add("validation-text");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setManaged(false);

        Button btnGenerate = new Button("Generer");
        btnGenerate.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Annuler");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox actions = new HBox(10, btnCancel, btnGenerate, progress);
        actions.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(
                new Label("Candidat"), candidatCombo, candidatErr,
                new Label("Type"), typeCombo, typeErr,
                new Label("Nombre de questions"), nbSpinner, nbErr,
                new Label("Difficulte"), difficulteCombo, diffErr,
                new Label("Description"), descriptionArea, descErr,
                actions
        );

        Runnable validate = () -> validateAiDialogAndToggle(candidatCombo, candidatErr, typeCombo, typeErr,
                nbSpinner, nbErr, difficulteCombo, diffErr, descriptionArea, descErr, btnGenerate);

        candidatCombo.valueProperty().addListener((obs, o, n) -> validate.run());
        typeCombo.valueProperty().addListener((obs, o, n) -> validate.run());
        difficulteCombo.valueProperty().addListener((obs, o, n) -> validate.run());
        descriptionArea.textProperty().addListener((obs, o, n) -> validate.run());
        nbSpinner.valueProperty().addListener((obs, o, n) -> validate.run());

        btnCancel.setOnAction(e -> dialog.close());

        btnGenerate.setOnAction(e -> {
            validate.run();
            if (btnGenerate.isDisable()) {
                return;
            }

            CandidatItem candidat = candidatCombo.getValue();
            AiTestGeneratorService.GenerationRequest req = new AiTestGeneratorService.GenerationRequest(
                    candidat.getId(),
                    typeCombo.getValue(),
                    safeSpinnerValue(nbSpinner),
                    difficulteCombo.getValue(),
                    descriptionArea.getText().trim()
            );

            btnGenerate.setDisable(true);
            btnCancel.setDisable(true);
            progress.setVisible(true);
            progress.setManaged(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    AiTestGeneratorService.GeneratedTestPayload payload = aiService.generate(req);
                    return testService.insertGeneratedTest(candidat.getId(), payload);
                }
            };

            task.setOnSucceeded(ev -> {
                progress.setVisible(false);
                progress.setManaged(false);
                btnGenerate.setDisable(false);
                btnCancel.setDisable(false);
                Boolean ok = task.getValue();
                if (Boolean.TRUE.equals(ok)) {
                    showInfo("Succes", "Test genere et insere avec succes.");
                    dialog.close();
                    loadTests();
                } else {
                    showError("Erreur", "Generation terminee mais insertion echouee.");
                }
            });

            task.setOnFailed(ev -> {
                progress.setVisible(false);
                progress.setManaged(false);
                btnGenerate.setDisable(false);
                btnCancel.setDisable(false);
                Throwable ex = task.getException();
                showError("Erreur IA", messageOf(ex));
            });

            Thread thread = new Thread(task, "ai-test-generator");
            thread.setDaemon(true);
            thread.start();
        });

        validate.run();

        Scene scene = new Scene(content, 520, 680);
        scene.getStylesheets().add(getClass().getResource("/testQuiz/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void handleLangFr() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/test_list.fxml"));
            Parent view = loader.load();
            navigateInParent(view);
        } catch (IOException e) {
            showError("Traduction", "Impossible de recharger l'interface en FR.");
        }
    }

    @FXML
    private void handleLangEn() {
        translateUi("EN");
    }

    @FXML
    private void handleLangAr() {
        translateUi("AR");
    }

    private void validateAiDialogAndToggle(
            ComboBox<CandidatItem> candidatCombo,
            Label candidatErr,
            ComboBox<String> typeCombo,
            Label typeErr,
            Spinner<Integer> nbSpinner,
            Label nbErr,
            ComboBox<String> difficulteCombo,
            Label diffErr,
            TextArea descriptionArea,
            Label descErr,
            Button btnGenerate
    ) {
        boolean cOk = candidatCombo.getValue() != null;
        boolean tOk = typeCombo.getValue() != null && !typeCombo.getValue().isBlank();
        int count = safeSpinnerValue(nbSpinner);
        boolean nOk = count >= 5 && count <= 30;
        boolean dOk = difficulteCombo.getValue() != null && !difficulteCombo.getValue().isBlank();
        boolean descOk = descriptionArea.getText() != null && descriptionArea.getText().trim().length() >= 10;

        updateValidationUI(candidatCombo, candidatErr, cOk, "Candidat obligatoire");
        updateValidationUI(typeCombo, typeErr, tOk, "Type obligatoire");
        updateValidationUI(nbSpinner, nbErr, nOk, "Entre 5 et 30");
        updateValidationUI(difficulteCombo, diffErr, dOk, "Difficulte obligatoire");
        updateValidationUI(descriptionArea, descErr, descOk, "Description obligatoire (min 10)");

        btnGenerate.setDisable(!(cOk && tOk && nOk && dOk && descOk));
    }

    private int safeSpinnerValue(Spinner<Integer> spinner) {
        try {
            Integer value = spinner.getValue();
            if (value != null) return value;
            String text = spinner.getEditor().getText();
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private String messageOf(Throwable throwable) {
        if (throwable == null) return "Erreur inconnue.";
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof NumberFormatException) {
            return "Reponse IA invalide (format numerique). Veuillez relancer la generation.";
        }
        String msg = root.getMessage();
        if (msg != null && !msg.isBlank()) return msg;
        return throwable.getClass().getSimpleName();
    }

    private void loadTests() {
        Integer candidatID = null;
        String candidatRaw = searchCandidatField.getText() == null ? "" : searchCandidatField.getText().trim();

        if (!candidatRaw.isEmpty()) {
            try {
                candidatID = Integer.parseInt(candidatRaw);
            } catch (NumberFormatException ignored) {
                candidatID = -1;
            }
        }

        List<Test> tests = testService.searchTests(
                searchTitreField.getText(),
                searchTypeField.getText(),
                candidatID
        );
        renderCards(tests);
    }

    private void renderCards(List<Test> tests) {
        testsFlow.getChildren().clear();

        if (tests == null || tests.isEmpty()) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);

        for (Test test : tests) {
            VBox card = createCard(test);
            testsFlow.getChildren().add(card);
        }
        refreshCardsSelection();
    }

    private VBox createCard(Test test) {
        Label idChip = new Label("#" + test.getId());
        idChip.getStyleClass().add("test-chip");

        Label typeChip = new Label(test.getType());
        typeChip.getStyleClass().add("test-chip-alt");
        typeChip.getStyleClass().add(typeBadgeClass(test.getType()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, idChip, typeChip, spacer);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(test.getTitre());
        title.getStyleClass().add("test-card-title");

        Label meta = new Label("CandidatID: " + test.getCandidatID() + "  |  Duration: " + test.getDurationSeconds() + "s");
        meta.getStyleClass().add("test-card-meta");

        Label score = new Label("ScoreMax: " + test.getScoreMax() + "  |  TotalPoints: " + test.getTotalPoints());
        score.getStyleClass().add("test-card-meta");

        Button edit = new Button("Modifier");
        edit.getStyleClass().addAll("btn-secondary", "btn-with-icon");
        edit.setGraphic(icon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25Zm18.71-11.04a1 1 0 0 0 0-1.41l-2.5-2.5a1 1 0 0 0-1.41 0l-1.83 1.83l3.75 3.75l1.99-1.67Z"));
        edit.setOnAction(e -> {
            selectedTest = test;
            fillForm(test);
            setEditMode(test.getId());
            refreshCardsSelection();
        });

        Button details = new Button("Supprimer");
        details.getStyleClass().addAll("btn-danger", "btn-with-icon");
        details.setGraphic(icon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3-9h2v8H9v-8zm4 0h2v8h-2v-8zM15.5 4l-1-1h-5l-1 1H5v2h14V4h-3.5z"));
        details.setOnAction(e -> handleDeleteFromCard(test));

        HBox actions = new HBox(8, edit, details);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, top, title, meta, score, actions);
        card.getStyleClass().add("test-card-modern");
        card.setPadding(new Insets(12));
        card.setPrefWidth(340);
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                selectedTest = test;
                openQuestionsFor(test);
            } else {
                selectedTest = test;
                fillForm(test);
                setEditMode(test.getId());
                refreshCardsSelection();
            }
        });
        card.setUserData(test.getId());
        installCardHoverAnimation(card);

        return card;
    }

    private SVGPath icon(String path) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("btn-icon-path");
        return icon;
    }

    private String typeBadgeClass(String type) {
        if (type == null) return "type-badge-default";
        String t = type.trim().toLowerCase();
        if (t.contains("qcm")) return "type-badge-qcm";
        if (t.contains("tech")) return "type-badge-tech";
        if (t.contains("log")) return "type-badge-logic";
        return "type-badge-default";
    }

    private void installCardHoverAnimation(VBox card) {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(140), card);
        scaleIn.setToX(1.015);
        scaleIn.setToY(1.015);
        TranslateTransition up = new TranslateTransition(Duration.millis(140), card);
        up.setToY(-3);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(140), card);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);
        TranslateTransition down = new TranslateTransition(Duration.millis(140), card);
        down.setToY(0);

        card.setOnMouseEntered(e -> {
            scaleOut.stop();
            down.stop();
            scaleIn.playFromStart();
            up.playFromStart();
        });
        card.setOnMouseExited(e -> {
            scaleIn.stop();
            up.stop();
            scaleOut.playFromStart();
            down.playFromStart();
        });
    }

    private Test buildTestFromForm() {
        CandidatItem selectedCandidate = formCandidatCombo.getValue();
        double scoreMax;
        int durationSeconds;
        int totalPoints;

        try {
            scoreMax = Double.parseDouble(valueOf(formScoreMaxField));
            durationSeconds = Integer.parseInt(valueOf(formDurationSecondsField));
            totalPoints = Integer.parseInt(valueOf(formTotalPointsField));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Verifier les champs numeriques du formulaire.");
        }

        String titre = valueOf(formTitreField);
        String type = formTypeCombo.getValue() == null ? "" : formTypeCombo.getValue().trim();

        if (selectedCandidate == null) throw new IllegalArgumentException("Le candidat est obligatoire.");
        if (titre.length() < 3) throw new IllegalArgumentException("Le titre est obligatoire (min 3).");
        if (type.isEmpty()) throw new IllegalArgumentException("Le type est obligatoire.");
        if (scoreMax < 0 || durationSeconds < 0 || totalPoints < 0) {
            throw new IllegalArgumentException("scoreMax, durationSeconds et totalPoints doivent etre >= 0.");
        }
        if (!testService.candidatExists(selectedCandidate.getId())) {
            throw new IllegalArgumentException("Le candidat selectionne n'existe pas dans la table user.");
        }

        int excludeId = selectedTest == null ? 0 : selectedTest.getId();
        if (testService.existsTitreForCandidat(selectedCandidate.getId(), titre, excludeId)) {
            throw new IllegalArgumentException("Ce candidat a deja un test avec ce titre.");
        }

        return new Test(selectedCandidate.getId(), titre, type, scoreMax, durationSeconds, totalPoints);
    }

    private void handleDeleteFromCard(Test test) {
        if (test == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce test ? Les questions et propositions liees seront supprimees (CASCADE).");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (testService.deleteTest(test.getId())) {
                if (selectedTest != null && selectedTest.getId() == test.getId()) {
                    selectedTest = null;
                    clearForm();
                    setAddMode();
                }
                showInfo("Succes", "Test supprime avec succes.");
                loadTests();
            } else {
                showError("Erreur", "Impossible de supprimer le test.");
            }
        }
    }

    private void openQuestionsFor(Test test) {
        if (test == null) return;
        selectedTest = test;
        handleQuestions();
    }

    private void refreshCardsSelection() {
        for (var node : testsFlow.getChildren()) {
            node.getStyleClass().remove("test-card-selected");
            if (selectedTest != null && node.getUserData() instanceof Integer id && id == selectedTest.getId()) {
                node.getStyleClass().add("test-card-selected");
            }
        }
    }

    private void setAddMode() {
        formMode = FormMode.ADD;
        editingTestId = null;
        formModeLabel.setText("Mode: Ajout");
    }

    private void setEditMode(int id) {
        formMode = FormMode.EDIT;
        editingTestId = id;
        formModeLabel.setText("Mode: Modification #" + id);
    }

    private void fillForm(Test test) {
        selectCandidateById(test.getCandidatID());
        formTitreField.setText(test.getTitre());
        formTypeCombo.setValue(test.getType());
        formScoreMaxField.setText(String.valueOf(test.getScoreMax()));
        formDurationSecondsField.setText(String.valueOf(test.getDurationSeconds()));
        formTotalPointsField.setText(String.valueOf(test.getTotalPoints()));
        validateFormAndToggleActions();
    }

    private void clearForm() {
        formCandidatCombo.setValue(null);
        formTitreField.clear();
        formTypeCombo.setValue(null);
        formScoreMaxField.clear();
        formDurationSecondsField.clear();
        formTotalPointsField.clear();
        clearValidationLabels();
        validateFormAndToggleActions();
    }

    private String valueOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void initCombos() {
        formTypeCombo.getItems().setAll(TEST_TYPES);
        formCandidatCombo.getItems().setAll(testService.getAllCandidats());
    }

    private void setupLiveValidation() {
        formTitreField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formScoreMaxField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formDurationSecondsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formTotalPointsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formCandidatCombo.valueProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formTypeCombo.valueProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
    }

    private void validateFormAndToggleActions() {
        boolean candidateOk = formCandidatCombo.getValue() != null;
        boolean titreOk = valueOf(formTitreField).length() >= 3;
        boolean typeOk = formTypeCombo.getValue() != null && !formTypeCombo.getValue().isBlank();
        boolean scoreOk = isDoubleNonNegative(formScoreMaxField);
        boolean durationOk = isIntNonNegative(formDurationSecondsField);
        boolean totalOk = isIntNonNegative(formTotalPointsField);

        updateValidationUI(formCandidatCombo, candidateValidationLabel, candidateOk, "Candidat obligatoire");
        updateValidationUI(formTitreField, titreValidationLabel, titreOk, "Titre min 3 caracteres");
        updateValidationUI(formTypeCombo, typeValidationLabel, typeOk, "Type obligatoire");
        updateValidationUI(formScoreMaxField, scoreValidationLabel, scoreOk, "ScoreMax >= 0");
        updateValidationUI(formDurationSecondsField, durationValidationLabel, durationOk, "Duration >= 0");
        updateValidationUI(formTotalPointsField, totalPointsValidationLabel, totalOk, "TotalPoints >= 0");

        boolean uniqOk = true;
        CandidatItem c = formCandidatCombo.getValue();
        String t = valueOf(formTitreField);
        if (candidateOk && titreOk && c != null) {
            int excludeId = selectedTest == null ? 0 : selectedTest.getId();
            uniqOk = !testService.existsTitreForCandidat(c.getId(), t, excludeId);
            if (!uniqOk) {
                updateValidationUI(formTitreField, titreValidationLabel, false, "Ce candidat a deja un test avec ce titre.");
            }
        }

        boolean formOk = candidateOk && titreOk && typeOk && scoreOk && durationOk && totalOk && uniqOk;
        if (btnAjouter != null) btnAjouter.setDisable(!formOk);
        if (btnModifier != null) btnModifier.setDisable(!formOk || selectedTest == null);
    }

    private void updateValidationUI(javafx.scene.Node field, Label messageLabel, boolean ok, String errMsg) {
        field.getStyleClass().removeAll("field-error", "field-ok");
        messageLabel.getStyleClass().removeAll("error-text", "ok-text");
        if (ok) {
            field.getStyleClass().add("field-ok");
            messageLabel.setText("OK");
            messageLabel.getStyleClass().add("ok-text");
        } else {
            field.getStyleClass().add("field-error");
            messageLabel.setText(errMsg);
            messageLabel.getStyleClass().add("error-text");
        }
    }

    private boolean isDoubleNonNegative(TextField field) {
        try {
            return Double.parseDouble(valueOf(field)) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIntNonNegative(TextField field) {
        try {
            return Integer.parseInt(valueOf(field)) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void clearValidationLabels() {
        Label[] labels = new Label[]{
                candidateValidationLabel, titreValidationLabel, typeValidationLabel,
                scoreValidationLabel, durationValidationLabel, totalPointsValidationLabel
        };
        for (Label l : labels) {
            if (l != null) l.setText("");
        }
    }

    private void selectCandidateById(int id) {
        for (CandidatItem item : formCandidatCombo.getItems()) {
            if (item.getId() == id) {
                formCandidatCombo.setValue(item);
                return;
            }
        }
        formCandidatCombo.setValue(null);
    }

    private void navigateInParent(Parent view) {
        if (rootPane.getParent() instanceof StackPane parent) {
            parent.getChildren().setAll(view);
        }
    }

    private void translateUi(String targetLang) {
        if (rootPane == null) {
            return;
        }

        Task<java.util.Map<String, String>> task = uiTranslator.buildTranslateTask(rootPane, targetLang);
        setLangButtonsDisabled(true);

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            uiTranslator.applyTranslations(rootPane, task.getValue());
            setLangButtonsDisabled(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLangButtonsDisabled(false);
            String message = task.getException() == null ? "Echec de traduction." : task.getException().getMessage();
            showError("Traduction", message == null || message.isBlank() ? "Echec de traduction." : message);
        }));

        Thread thread = new Thread(task, "tests-ui-translation");
        thread.setDaemon(true);
        thread.start();
    }

    private void setLangButtonsDisabled(boolean disabled) {
        if (langFrButton != null) langFrButton.setDisable(disabled);
        if (langEnButton != null) langEnButton.setDisable(disabled);
        if (langArButton != null) langArButton.setDisable(disabled);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
