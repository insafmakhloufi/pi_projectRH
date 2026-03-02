package Utils.front;

import Controllers.testQuizFront.QuizController;
import Controllers.testQuizFront.FrontResultViewController;
import Entities.testQuiz.Test;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public final class FrontNavigator {

    private static StackPane contentArea;

    private FrontNavigator() {
    }

    public static void configure(StackPane area) {
        contentArea = area;
    }

    public static void showTestsList() {
        setContent(load("/testQuizFront/fxml/tests_list.fxml"));
    }

    public static void showQuiz(Test test) {
        try {
            FXMLLoader loader = new FXMLLoader(FrontNavigator.class.getResource("/testQuizFront/quiz_view.fxml"));
            Parent root = loader.load();
            QuizController controller = loader.getController();
            controller.setTest(test);
            setContent(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger quiz_view.fxml", e);
        }
    }

    public static void showResult(FrontQuizResult result) {
        try {
            FXMLLoader loader = new FXMLLoader(FrontNavigator.class.getResource("/testQuizFront/fxml/result_view.fxml"));
            Parent root = loader.load();
            FrontResultViewController controller = loader.getController();
            controller.setResult(result);
            setContent(root);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger result_view.fxml", e);
        }
    }

    private static Parent load(String path) {
        try {
            return FXMLLoader.load(FrontNavigator.class.getResource(path));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger " + path, e);
        }
    }

    private static void setContent(Parent root) {
        if (contentArea == null) {
            throw new IllegalStateException("FrontNavigator non configure.");
        }
        contentArea.getChildren().setAll(root);
    }
}
