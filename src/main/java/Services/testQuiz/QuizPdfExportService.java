package Services.testQuiz;

import Utils.Session;
import Utils.front.FrontQuizQuestion;
import Utils.front.FrontQuizResult;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class QuizPdfExportService {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportToPdf(FrontQuizResult result, File outputFile) throws IOException {
        if (result == null || outputFile == null) {
            throw new IOException("Result or output file is null");
        }

        Document document = new Document(PageSize.A4, 48, 48, 50, 40);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter.getInstance(document, fos);
            document.open();
            try {
                addHeader(document);
                addGlobalInfo(document, result);
                addSeparator(document);
                addQuestions(document, result);
                addFooter(document);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
            String detail = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw new IOException("Unable to build PDF - " + detail, e);
        }
    }

    public static void exportWithChooser(FrontQuizResult result, Window owner) {
        if (result == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter resultat PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        String testTitle = result.getTest() != null && result.getTest().getTitre() != null
                ? result.getTest().getTitre().trim().replaceAll("[^a-zA-Z0-9-_ ]", "_")
                : "resultat_test";
        if (testTitle.isBlank()) testTitle = "resultat_test";
        chooser.setInitialFileName(testTitle + ".pdf");

        File selected;
        try {
            selected = showSaveDialogOnFxThread(chooser, owner);
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getClass().getSimpleName()
                    + ": " + e.getMessage();
            showError("Export PDF", "Impossible d'exporter le PDF\n\n" + detail);
            return;
        }
        if (selected == null) return;

        try {
            new QuizPdfExportService().exportToPdf(result, selected);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export PDF");
            ok.setHeaderText(null);
            ok.setContentText("PDF exporte avec succes.");
            ok.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getClass().getSimpleName()
                    + ": " + e.getMessage();
            showError("Export PDF", "Impossible d'exporter le PDF\n\n" + detail);
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Paragraph p = new Paragraph("CareerLink - Resultat du Test",
                new Font(Font.HELVETICA, 18, Font.BOLD, new Color(18, 70, 140)));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(14);
        document.add(p);
    }

    private void addGlobalInfo(Document document, FrontQuizResult result) throws DocumentException {
        String candidat = Session.getCurrentUser() != null && Session.getCurrentUser().getFullName() != null
                ? Session.getCurrentUser().getFullName().trim()
                : "N/A";
        String titre = result.getTest() != null ? safe(result.getTest().getTitre()) : "N/A";
        String type = result.getTest() != null ? safe(result.getTest().getType()) : "N/A";
        LocalDateTime passed = result.getDatePassed() == null ? LocalDateTime.now() : result.getDatePassed();

        addInfoLine(document, "Candidat", candidat);
        addInfoLine(document, "Test", titre);
        addInfoLine(document, "Type", type);
        addInfoLine(document, "Date passage", passed.format(DT_FORMAT));

        document.add(Chunk.NEWLINE);
        int percent = result.percent();
        addInfoLine(document, "Score global", result.getScore() + " / " + result.getMaxScore() + " points (" + percent + "%)");
        addInfoLine(document, "Stats", "Correct=" + result.getCorrect()
                + " | Wrong=" + result.getWrong()
                + " | Unanswered=" + result.getUnanswered()
                + " | Temps=" + formatTime(result.getTimeSpentSeconds()));
        addInfoLine(document, "Mention", result.gradeMessage());
    }

    private void addQuestions(Document document, FrontQuizResult result) throws DocumentException {
        List<FrontQuizQuestion> questions = result.getQuestions();
        if (questions == null) questions = List.of();
        if (questions.isEmpty()) {
            Paragraph empty = new Paragraph("Aucune question detaillee disponible.",
                    new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY));
            document.add(empty);
            return;
        }

        Integer[] selectedAnswers = result.getSelectedAnswers();
        String[] openAnswers = result.getOpenAnswers();
        for (int i = 0; i < questions.size(); i++) {
            FrontQuizQuestion q = questions.get(i);
            document.add(Chunk.NEWLINE);
            Paragraph qTitle = new Paragraph((i + 1) + ". " + safe(q.getText()),
                    new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK));
            document.add(qTitle);

            String candidateAnswer = resolveCandidateAnswer(q, selectedAnswers, openAnswers, i);
            String correctAnswer = resolveCorrectAnswer(q);
            String status = resolveStatus(q, selectedAnswers, openAnswers, i);

            addInfoLine(document, "Reponse candidat", candidateAnswer);
            addInfoLine(document, "Reponse correcte", correctAnswer);
            addInfoLine(document, "Resultat", status);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        addSeparator(document);
        Paragraph p = new Paragraph("Export genere le " + LocalDateTime.now().format(DT_FORMAT),
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY));
        p.setAlignment(Element.ALIGN_RIGHT);
        document.add(p);
    }

    private void addSeparator(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(210, 210, 210));
        line.setLineWidth(1);
        document.add(new Chunk(line));
        document.add(Chunk.NEWLINE);
    }

    private void addInfoLine(Document document, String label, String value) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", new Font(Font.HELVETICA, 10, Font.BOLD)));
        p.add(new Chunk(safe(value), new Font(Font.HELVETICA, 10, Font.NORMAL)));
        p.setSpacingAfter(3);
        document.add(p);
    }

    private String resolveCandidateAnswer(FrontQuizQuestion q, Integer[] selectedAnswers, String[] openAnswers, int i) {
        if (q.isCodingQuestion() || q.isOpenQuestion()) {
            if (openAnswers == null || i >= openAnswers.length || openAnswers[i] == null || openAnswers[i].isBlank()) {
                return "Non repondu";
            }
            return openAnswers[i].trim();
        }

        Integer selected = (selectedAnswers != null && i < selectedAnswers.length) ? selectedAnswers[i] : null;
        if (selected == null || selected < 0 || selected >= q.getOptions().size()) {
            return "Non repondu";
        }
        String option = q.getOptions().get(selected);
        return letter(selected) + ") " + safe(option);
    }

    private String resolveCorrectAnswer(FrontQuizQuestion q) {
        if (q.isCodingQuestion() || q.isOpenQuestion()) {
            return safe(q.getCorrectAnswer());
        }
        int idx = q.getCorrectIndex();
        if (idx >= 0 && idx < q.getOptions().size()) {
            return letter(idx) + ") " + safe(q.getOptions().get(idx));
        }
        return "N/A";
    }

    private String resolveStatus(FrontQuizQuestion q, Integer[] selectedAnswers, String[] openAnswers, int i) {
        if (q.isCodingQuestion() || q.isOpenQuestion()) {
            String candidate = (openAnswers != null && i < openAnswers.length) ? safe(openAnswers[i]).trim() : "";
            if (candidate.isBlank()) return "- Ignore";
            boolean ok = candidate.equalsIgnoreCase(safe(q.getCorrectAnswer()).trim());
            return ok ? "Correct" : "Incorrect";
        }

        Integer selected = (selectedAnswers != null && i < selectedAnswers.length) ? selectedAnswers[i] : null;
        if (selected == null || selected < 0) return "- Ignore";
        return selected == q.getCorrectIndex() ? "Correct" : "Incorrect";
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String letter(int i) {
        return switch (i) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> "?";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static File showSaveDialogOnFxThread(FileChooser chooser, Window owner) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            return chooser.showSaveDialog(owner);
        }
        final File[] selected = new File[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                selected[0] = chooser.showSaveDialog(owner);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        return selected[0];
    }

    private static void showError(String title, String message) {
        Alert ko = new Alert(Alert.AlertType.ERROR);
        ko.setTitle(title);
        ko.setHeaderText("Echec export PDF");
        ko.setContentText(message);
        ko.showAndWait();
    }
}
