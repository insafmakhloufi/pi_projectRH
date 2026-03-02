package Utils;

import Services.ai.OpenAiTranslationService;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UiTranslator {

    private final OpenAiTranslationService translationService = new OpenAiTranslationService();
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public Task<Map<String, String>> buildTranslateTask(Parent root, String targetLang) {
        Set<String> texts = new LinkedHashSet<>();
        collectTexts(root, texts);

        return new Task<>() {
            @Override
            protected Map<String, String> call() {
                Map<String, String> result = new ConcurrentHashMap<>();
                int total = Math.max(texts.size(), 1);
                int index = 0;
                for (String text : texts) {
                    if (isCancelled()) {
                        break;
                    }
                    String key = targetLang + "||" + text;
                    String translated = cache.get(key);
                    if (translated == null) {
                        translated = translationService.translate(text, "AUTO", targetLang);
                        cache.put(key, translated);
                    }
                    result.put(text, translated);
                    index++;
                    updateProgress(index, total);
                }
                return result;
            }
        };
    }

    public void applyTranslations(Parent root, Map<String, String> translations) {
        applyNode(root, translations);
    }

    private void collectTexts(Node node, Set<String> sink) {
        if (node instanceof Labeled labeled) {
            addIfTranslatable(labeled.getText(), sink);
        }
        if (node instanceof TextInputControl input) {
            addIfTranslatable(input.getPromptText(), sink);
        }
        if (node instanceof ComboBox<?> comboBox) {
            addIfTranslatable(comboBox.getPromptText(), sink);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectTexts(child, sink);
            }
        }
    }

    private void applyNode(Node node, Map<String, String> translations) {
        if (node instanceof Labeled labeled) {
            labeled.setText(translateValue(labeled.getText(), translations));
        }
        if (node instanceof TextInputControl input) {
            input.setPromptText(translateValue(input.getPromptText(), translations));
        }
        if (node instanceof ComboBox<?> comboBox) {
            comboBox.setPromptText(translateValue(comboBox.getPromptText(), translations));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyNode(child, translations);
            }
        }
    }

    private void addIfTranslatable(String text, Set<String> sink) {
        if (text == null) {
            return;
        }
        String trimmed = text.trim();
        if (shouldTranslate(trimmed)) {
            sink.add(trimmed);
        }
    }

    private String translateValue(String original, Map<String, String> translations) {
        if (original == null) {
            return null;
        }
        String trimmed = original.trim();
        if (!shouldTranslate(trimmed)) {
            return original;
        }
        String translated = translations.get(trimmed);
        return translated == null || translated.isBlank() ? original : translated;
    }

    private boolean shouldTranslate(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String upper = text.toUpperCase(Locale.ROOT).trim();
        if ("FR".equals(upper) || "EN".equals(upper) || "AR".equals(upper)) {
            return false;
        }
        if (text.length() <= 3 && text.equals(upper)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
