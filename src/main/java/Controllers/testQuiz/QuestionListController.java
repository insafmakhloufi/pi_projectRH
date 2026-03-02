package Controllers.testQuiz;

import Entities.testQuiz.Proposition;
import Entities.testQuiz.Question;
import Entities.testQuiz.Test;
import Services.ai.OpenAiImageService;
import Services.testQuiz.PropositionService;
import Services.testQuiz.QuestionService;
import Utils.QuestionImageStore;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class QuestionListController implements Initializable {

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox leftBlock;
    @FXML
    private VBox rightBlock;
    @FXML
    private Label testInfoLabel;
    @FXML
    private Label formModeLabel;
    @FXML
    private Label questionValidationLabel;
    @FXML
    private Label pointsValidationLabel;
    @FXML
    private Label propositionsValidationLabel;
    @FXML
    private TextArea formQuestionArea;
    @FXML
    private TextField formReponseField;
    @FXML
    private TextField formPointsField;
    @FXML
    private TextField formProp1Field;
    @FXML
    private TextField formProp2Field;
    @FXML
    private TextField formProp3Field;
    @FXML
    private TextField formProp4Field;
    @FXML
    private ComboBox<String> formCorrectPropCombo;
    @FXML
    private VBox nonQcmResponseBox;
    @FXML
    private VBox qcmPropositionsBox;
    @FXML
    private TextField searchQuestionField;
    @FXML
    private TextField searchReponseField;
    @FXML
    private TextField searchPointsField;
    @FXML
    private FlowPane questionsFlow;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private ImageView formQuestionImageView;
    @FXML
    private Label formImagePlaceholderLabel;
    @FXML
    private TextArea formImagePromptArea;
    @FXML
    private Button btnGenerateImage;
    @FXML
    private ProgressIndicator imageGenerationProgress;
    @FXML
    private Label imageValidationLabel;
    @FXML
    private Button langFrButton;
    @FXML
    private Button langEnButton;
    @FXML
    private Button langArButton;

    private final QuestionService questionService = new QuestionService();
    private final PropositionService propositionService = new PropositionService();
    private final OpenAiImageService openAiImageService = new OpenAiImageService();
    private final QuestionImageStore questionImageStore = new QuestionImageStore();
    private final UiTranslator uiTranslator = new UiTranslator();
    private Test selectedTest;
    private Question selectedQuestion;
    private List<Question> questionsCache = new ArrayList<>();
    private byte[] pendingImageBytes;
    private boolean removeImageRequested;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        formCorrectPropCombo.getItems().setAll("1", "2", "3", "4");

        searchQuestionField.textProperty().addListener((obs, o, n) -> applyFilters());
        searchReponseField.textProperty().addListener((obs, o, n) -> applyFilters());
        searchPointsField.textProperty().addListener((obs, o, n) -> applyFilters());

        formQuestionArea.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formReponseField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formPointsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formProp1Field.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formProp2Field.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formProp3Field.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formProp4Field.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        formCorrectPropCombo.valueProperty().addListener((obs, o, n) -> validateFormAndToggleActions());

        clearForm();
        updateTypeSpecificFieldsVisibility();
        validateFormAndToggleActions();
    }

    public void setSelectedTest(Test selectedTest) {
        this.selectedTest = selectedTest;
        if (selectedTest != null) {
            testInfoLabel.setText("Questions du Test #" + selectedTest.getId() + " - " + selectedTest.getTitre());
        }
        updateTypeSpecificFieldsVisibility();
        loadQuestions();
    }

    @FXML
    private void handleAjouter() {
        if (selectedTest == null) {
            showError("Contexte", "Aucun test selectionne.");
            return;
        }
        try {
            Question question = buildQuestionFromForm();

            if (isQcmMode()) {
                int newQuestionId = questionService.addQuestionAndReturnId(question);
                if (newQuestionId <= 0) {
                    showError("Erreur", "Impossible d'ajouter la question.");
                    return;
                }
                if (!saveQcmPropositions(newQuestionId)) {
                    questionService.deleteQuestion(newQuestionId);
                    showError("Erreur", "Question ajoutee mais echec enregistrement propositions.");
                    return;
                }

                persistQuestionImage(newQuestionId);
            } else {
                int newQuestionId = questionService.addQuestionAndReturnId(question);
                if (newQuestionId <= 0) {
                    showError("Erreur", "Impossible d'ajouter la question.");
                    return;
                }
                persistQuestionImage(newQuestionId);
            }

            showInfo("Succes", "Question ajoutee avec succes.");
            selectedQuestion = null;
            clearForm();
            setAddMode();
            loadQuestions();
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (selectedQuestion == null) {
            showError("Validation", "Selectionnez une question a modifier.");
            return;
        }
        try {
            Question form = buildQuestionFromForm();
            Question question = new Question(
                    selectedQuestion.getId(),
                    selectedTest.getId(),
                    form.getQuestion(),
                    form.getReponse(),
                    form.getPoints()
            );

            if (!questionService.updateQuestion(question)) {
                showError("Erreur", "Impossible de modifier la question.");
                return;
            }

            if (isQcmMode() && !saveQcmPropositions(selectedQuestion.getId())) {
                showError("Erreur", "Question modifiee mais echec mise a jour propositions.");
                return;
            }

            persistQuestionImage(selectedQuestion.getId());

            showInfo("Succes", "Question modifiee avec succes.");
            selectedQuestion = null;
            clearForm();
            setAddMode();
            loadQuestions();
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedQuestion == null) {
            showError("Validation", "Selectionnez une question a supprimer.");
            return;
        }
        handleDeleteFromCard(selectedQuestion);
    }

    @FXML
    private void handleAnnulerForm() {
        selectedQuestion = null;
        clearForm();
        setAddMode();
        refreshCardsSelection();
    }

    @FXML
    private void handleRafraichir() {
        loadQuestions();
    }

    @FXML
    private void handleRetourTests() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/testQuiz/fxml/test_list.fxml"));
            navigateInParent(view);
        } catch (IOException e) {
            showError("Erreur", "Impossible de retourner vers Tests.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLangFr() {
        if (selectedTest == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/question_list.fxml"));
            Parent view = loader.load();
            QuestionListController controller = loader.getController();
            controller.setSelectedTest(selectedTest);
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

    @FXML
    private void handleImporterImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer une image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File file = chooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            pendingImageBytes = Files.readAllBytes(file.toPath());
            removeImageRequested = false;
            setImagePreviewFromBytes(pendingImageBytes);
            setImageValidation("Image importee.");
        } catch (IOException e) {
            showError("Image", "Impossible de lire l'image selectionnee.");
        }
    }

    @FXML
    private void handleSupprimerImage() {
        pendingImageBytes = null;
        removeImageRequested = true;
        clearImagePreview();
        setImageValidation("Image supprimee.");
    }

    @FXML
    private void handleGenererImage() {
        String prompt = valueOf(formImagePromptArea);
        if (prompt.isBlank()) {
            prompt = valueOf(formQuestionArea);
        }
        if (prompt.isBlank()) {
            showError("Image IA", "Veuillez saisir un prompt ou une question.");
            return;
        }

        setImageGenerationBusy(true);
        String finalPrompt = prompt;
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() {
                return openAiImageService.generateImagePng(finalPrompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setImageGenerationBusy(false);
            pendingImageBytes = task.getValue();
            removeImageRequested = false;
            setImagePreviewFromBytes(pendingImageBytes);
            setImageValidation("Image generee par IA.");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setImageGenerationBusy(false);
            Throwable ex = task.getException();
            String msg = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Generation image impossible."
                    : ex.getMessage();
            showError("Image IA", msg);
        }));

        Thread thread = new Thread(task, "question-image-generator");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadQuestions() {
        if (selectedTest == null) {
            questionsCache = new ArrayList<>();
            renderCards(new ArrayList<>());
            return;
        }
        questionsCache = questionService.getByTestId(selectedTest.getId());
        applyFilters();
    }

    private void applyFilters() {
        String q = valueOf(searchQuestionField).toLowerCase();
        String r = valueOf(searchReponseField).toLowerCase();
        String p = valueOf(searchPointsField);

        Integer pointsFilter = null;
        if (!p.isEmpty()) {
            try {
                pointsFilter = Integer.parseInt(p);
            } catch (NumberFormatException ignored) {
                pointsFilter = -1;
            }
        }

        List<Question> filtered = new ArrayList<>();
        for (Question question : questionsCache) {
            boolean matchQ = q.isEmpty() || safe(question.getQuestion()).toLowerCase().contains(q);
            boolean matchR = r.isEmpty() || safe(question.getReponse()).toLowerCase().contains(r);
            boolean matchP = pointsFilter == null || question.getPoints() == pointsFilter;
            if (matchQ && matchR && matchP) {
                filtered.add(question);
            }
        }
        renderCards(filtered);
    }

    private void renderCards(List<Question> questions) {
        questionsFlow.getChildren().clear();

        if (questions == null || questions.isEmpty()) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);

        for (Question question : questions) {
            VBox card = createCard(question);
            questionsFlow.getChildren().add(card);
        }
        refreshCardsSelection();
    }

    private VBox createCard(Question question) {
        Label idChip = new Label("#" + question.getId());
        idChip.getStyleClass().add("test-chip");

        Label pointsChip = new Label("POINTS " + question.getPoints());
        pointsChip.getStyleClass().add("test-chip-alt");
        pointsChip.getStyleClass().add("type-badge-tech");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, idChip, pointsChip, spacer);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safe(question.getQuestion()));
        title.getStyleClass().add("test-card-title");
        title.setWrapText(true);
        title.setMaxWidth(300);

        String rep = safe(question.getReponse());
        Label meta = new Label("Reponse: " + (rep.isEmpty() ? "-" : rep));
        meta.getStyleClass().add("test-card-meta");

        Button edit = new Button("Modifier");
        edit.getStyleClass().addAll("btn-secondary", "btn-with-icon");
        edit.setGraphic(icon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25Zm18.71-11.04a1 1 0 0 0 0-1.41l-2.5-2.5a1 1 0 0 0-1.41 0l-1.83 1.83l3.75 3.75l1.99-1.67Z"));
        edit.setOnAction(e -> {
            selectedQuestion = question;
            fillForm(question);
            setEditMode(question.getId());
            refreshCardsSelection();
        });

        Button delete = new Button("Supprimer");
        delete.getStyleClass().addAll("btn-danger", "btn-with-icon");
        delete.setGraphic(icon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3-9h2v8H9v-8zm4 0h2v8h-2v-8zM15.5 4l-1-1h-5l-1 1H5v2h14V4h-3.5z"));
        delete.setOnAction(e -> handleDeleteFromCard(question));

        HBox actions = new HBox(8, edit, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, top, title, meta, actions);
        card.getStyleClass().add("test-card-modern");
        card.setPadding(new Insets(12));
        card.setPrefWidth(340);
        card.setOnMouseClicked(e -> {
            selectedQuestion = question;
            fillForm(question);
            setEditMode(question.getId());
            refreshCardsSelection();
        });
        card.setUserData(question.getId());
        installCardHoverAnimation(card);
        return card;
    }

    private SVGPath icon(String path) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("btn-icon-path");
        return icon;
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

    private void handleDeleteFromCard(Question question) {
        if (question == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer cette question ? Les propositions liees seront supprimees (CASCADE).");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (questionService.deleteQuestion(question.getId())) {
                if (selectedQuestion != null && selectedQuestion.getId() == question.getId()) {
                    selectedQuestion = null;
                    clearForm();
                    setAddMode();
                }
                showInfo("Succes", "Question supprimee avec succes.");
                loadQuestions();
            } else {
                showError("Erreur", "Impossible de supprimer la question.");
            }
        }
    }

    private Question buildQuestionFromForm() {
        if (selectedTest == null) {
            throw new IllegalArgumentException("Aucun test selectionne.");
        }

        String questionText = valueOf(formQuestionArea);
        String reponse = isQcmMode() ? "" : valueOf(formReponseField);
        int points;

        if (questionText.isEmpty()) {
            throw new IllegalArgumentException("La question est obligatoire.");
        }
        try {
            points = Integer.parseInt(valueOf(formPointsField));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le champ points doit etre numerique.");
        }
        if (points < 1) {
            throw new IllegalArgumentException("Le champ points doit etre >= 1.");
        }
        if (!questionService.testExists(selectedTest.getId())) {
            throw new IllegalArgumentException("Le test selectionne n'existe plus.");
        }

        if (isQcmMode() && !isQcmPropositionsValid()) {
            throw new IllegalArgumentException("QCM: au moins 2 propositions et une correcte sont obligatoires.");
        }

        return new Question(selectedTest.getId(), questionText, reponse.isEmpty() ? null : reponse, points);
    }

    private boolean saveQcmPropositions(int questionId) {
        List<Proposition> existing = propositionService.getByQuestionId(questionId);
        for (Proposition proposition : existing) {
            propositionService.deleteProposition(proposition.getId());
        }

        for (Proposition proposition : buildQcmPropositions(questionId)) {
            if (!propositionService.addProposition(proposition)) {
                return false;
            }
        }
        return true;
    }

    private List<Proposition> buildQcmPropositions(int questionId) {
        List<Proposition> propositions = new ArrayList<>();
        String[] values = new String[]{
                valueOf(formProp1Field),
                valueOf(formProp2Field),
                valueOf(formProp3Field),
                valueOf(formProp4Field)
        };
        int correctIndex = Integer.parseInt(formCorrectPropCombo.getValue());

        for (int i = 0; i < values.length; i++) {
            if (!values[i].isBlank()) {
                propositions.add(new Proposition(questionId, values[i], (i + 1) == correctIndex));
            }
        }
        return propositions;
    }

    private void setAddMode() {
        formModeLabel.setText("Mode: Ajout");
    }

    private void setEditMode(int id) {
        formModeLabel.setText("Mode: Modification #" + id);
    }

    private void fillForm(Question question) {
        formQuestionArea.setText(safe(question.getQuestion()));
        formReponseField.setText(safe(question.getReponse()));
        formPointsField.setText(String.valueOf(question.getPoints()));
        formImagePromptArea.clear();
        pendingImageBytes = null;
        removeImageRequested = false;
        loadExistingQuestionImage(question);

        clearQcmFields();
        if (isQcmMode()) {
            List<Proposition> propositions = propositionService.getByQuestionId(question.getId());
            int next = 0;
            Integer correct = null;
            for (Proposition proposition : propositions) {
                if (next == 0) formProp1Field.setText(safe(proposition.getContenu()));
                if (next == 1) formProp2Field.setText(safe(proposition.getContenu()));
                if (next == 2) formProp3Field.setText(safe(proposition.getContenu()));
                if (next == 3) formProp4Field.setText(safe(proposition.getContenu()));
                if (proposition.isEstCorrect() && correct == null) {
                    correct = next + 1;
                }
                next++;
                if (next >= 4) break;
            }
            formCorrectPropCombo.setValue(correct == null ? null : String.valueOf(correct));
        }
        validateFormAndToggleActions();
    }

    private void clearForm() {
        formQuestionArea.clear();
        formReponseField.clear();
        formPointsField.clear();
        clearQcmFields();
        questionValidationLabel.setText("");
        pointsValidationLabel.setText("");
        propositionsValidationLabel.setText("");
        formImagePromptArea.clear();
        pendingImageBytes = null;
        removeImageRequested = false;
        clearImagePreview();
        setImageValidation("");
        validateFormAndToggleActions();
    }

    private void clearQcmFields() {
        formProp1Field.clear();
        formProp2Field.clear();
        formProp3Field.clear();
        formProp4Field.clear();
        formCorrectPropCombo.setValue(null);
    }

    private void refreshCardsSelection() {
        for (var node : questionsFlow.getChildren()) {
            node.getStyleClass().remove("test-card-selected");
            if (selectedQuestion != null && node.getUserData() instanceof Integer id && id == selectedQuestion.getId()) {
                node.getStyleClass().add("test-card-selected");
            }
        }
    }

    private String valueOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String valueOf(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isQcmMode() {
        return selectedTest != null
                && selectedTest.getType() != null
                && selectedTest.getType().trim().equalsIgnoreCase("QCM");
    }

    private void updateTypeSpecificFieldsVisibility() {
        boolean qcm = isQcmMode();
        qcmPropositionsBox.setVisible(qcm);
        qcmPropositionsBox.setManaged(qcm);
        nonQcmResponseBox.setVisible(!qcm);
        nonQcmResponseBox.setManaged(!qcm);
    }

    private boolean isQcmPropositionsValid() {
        String p1 = valueOf(formProp1Field);
        String p2 = valueOf(formProp2Field);
        String p3 = valueOf(formProp3Field);
        String p4 = valueOf(formProp4Field);
        int count = 0;
        if (!p1.isBlank()) count++;
        if (!p2.isBlank()) count++;
        if (!p3.isBlank()) count++;
        if (!p4.isBlank()) count++;

        if (count < 2) return false;
        if (formCorrectPropCombo.getValue() == null) return false;

        int correctIndex;
        try {
            correctIndex = Integer.parseInt(formCorrectPropCombo.getValue());
        } catch (NumberFormatException e) {
            return false;
        }

        String[] values = new String[]{p1, p2, p3, p4};
        return correctIndex >= 1 && correctIndex <= 4 && !values[correctIndex - 1].isBlank();
    }

    private void navigateInParent(Parent view) {
        if (rootPane.getParent() instanceof StackPane parent) {
            parent.getChildren().setAll(view);
        }
    }

    private void persistQuestionImage(int questionId) {
        try {
            if (removeImageRequested) {
                questionImageStore.removeMapping(questionId);
                removeImageRequested = false;
            }
            if (pendingImageBytes != null && pendingImageBytes.length > 0) {
                Path saved = questionImageStore.saveGeneratedPng(questionId, pendingImageBytes);
                questionImageStore.saveMapping(questionId, saved);
                pendingImageBytes = null;
            }
        } catch (RuntimeException e) {
            showError("Image", e.getMessage() == null ? "Erreur de sauvegarde image." : e.getMessage());
        }
    }

    private void loadExistingQuestionImage(Question question) {
        if (question == null) {
            clearImagePreview();
            return;
        }
        Path path = questionImageStore.resolveImagePath(question.getId(), question.getReponse());
        if (path == null) {
            clearImagePreview();
            return;
        }
        try {
            Image image = new Image(path.toUri().toString(), false);
            if (image.isError()) {
                clearImagePreview();
                return;
            }
            formQuestionImageView.setImage(image);
            if (formImagePlaceholderLabel != null) {
                formImagePlaceholderLabel.setVisible(false);
            }
            setImageValidation("Image existante chargee.");
        } catch (Exception e) {
            clearImagePreview();
        }
    }

    private void setImagePreviewFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            clearImagePreview();
            return;
        }
        Image image = new Image(new ByteArrayInputStream(bytes));
        if (image.isError()) {
            clearImagePreview();
            showError("Image", "Format image invalide.");
            return;
        }
        formQuestionImageView.setImage(image);
        if (formImagePlaceholderLabel != null) {
            formImagePlaceholderLabel.setVisible(false);
        }
    }

    private void clearImagePreview() {
        if (formQuestionImageView != null) {
            formQuestionImageView.setImage(null);
        }
        if (formImagePlaceholderLabel != null) {
            formImagePlaceholderLabel.setVisible(true);
        }
    }

    private void setImageValidation(String message) {
        if (imageValidationLabel != null) {
            imageValidationLabel.setText(message == null ? "" : message);
        }
    }

    private void setImageGenerationBusy(boolean busy) {
        if (btnGenerateImage != null) btnGenerateImage.setDisable(busy);
        if (imageGenerationProgress != null) {
            imageGenerationProgress.setVisible(busy);
            imageGenerationProgress.setManaged(busy);
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

        Thread thread = new Thread(task, "question-ui-translation");
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

    private void validateFormAndToggleActions() {
        boolean qOk = valueOf(formQuestionArea).trim().length() >= 1;
        boolean pOk;
        try {
            pOk = Integer.parseInt(valueOf(formPointsField)) >= 1;
        } catch (Exception ignored) {
            pOk = false;
        }

        updateValidationUI(formQuestionArea, questionValidationLabel, qOk, "Question obligatoire");
        updateValidationUI(formPointsField, pointsValidationLabel, pOk, "Points >= 1");

        boolean qcmOk = true;
        if (isQcmMode()) {
            qcmOk = isQcmPropositionsValid();
            updateValidationUI(formCorrectPropCombo, propositionsValidationLabel, qcmOk,
                    "QCM: 2+ propositions et une correcte.");
        } else {
            formCorrectPropCombo.getStyleClass().removeAll("field-error", "field-ok");
            propositionsValidationLabel.getStyleClass().removeAll("error-text", "ok-text");
            propositionsValidationLabel.setText("");
        }

        boolean ok = qOk && pOk && selectedTest != null && qcmOk;
        if (btnAjouter != null) btnAjouter.setDisable(!ok);
        if (btnModifier != null) btnModifier.setDisable(!ok || selectedQuestion == null);
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
}
