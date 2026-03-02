package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Entretien;
import Services.candidature.EntretienService;
import Services.candidature.CandidatureService;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RH_EntretiensController {
    @FXML private DatePicker dpFilter;
    @FXML private Label lblMonth;
    @FXML private GridPane calendarGrid;

    @FXML private Label lblWeekRange;
    @FXML private GridPane weekGrid;

    @FXML private Label lblSelName;
    @FXML private Label lblSelSub;
    @FXML private Label lblSelTime;
    @FXML private Button btnSelDetails;
    @FXML private Button btnSelEdit;
    @FXML private Button btnSelDelete;

    @FXML private VBox entretienDetailsBox;
    @FXML private VBox entretienPlaceholder;

    // Champs conservés pour compat mais cachés dans le FXML
    @FXML private Label lblSummary;
    @FXML private VBox listBox;
    @FXML private ComboBox<Candidature> cbCandidat;
    @FXML private Label lblBlueTitle;


    private final EntretienService entretienService = new EntretienService();

    private Map<Integer, Candidature> candCache = new HashMap<>();

    private YearMonth currentMonth;

    private Integer selectedEntretienId = null;

    private LocalDate weekStart = null; // lundi

    private boolean autoJumpedToFirstEntretien = false;

    private static final int START_HOUR = 9;
    private static final int END_HOUR = 16;

    @FXML
    private void initialize() {
        currentMonth = YearMonth.now();

        if (weekGrid != null) {
            Rectangle clip = new Rectangle();
            clip.setArcWidth(52);
            clip.setArcHeight(52);
            weekGrid.setClip(clip);
            weekGrid.layoutBoundsProperty().addListener((obs, oldB, b) -> {
                if (b == null) return;
                clip.setWidth(b.getWidth());
                clip.setHeight(b.getHeight());
            });
        }

        dpFilter.setValue(LocalDate.now());
        dpFilter.valueProperty().addListener((obs, o, n) -> {
            updateWeekFromDate(n);
            updateCalendar();
            refreshWeekly();
        });

        updateCalendar();
        updateWeekFromDate(dpFilter.getValue());
        refreshWeekly();
        updateSelectedCard(null);
    }

    @FXML
    private void resetFilters() {
        dpFilter.setValue(LocalDate.now());
        currentMonth = YearMonth.now();
        updateCalendar();
        updateWeekFromDate(dpFilter.getValue());
        refreshWeekly();
    }

    @FXML
    private void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateCalendar();
    }

    @FXML
    private void prevWeek() {
        if (weekStart == null) updateWeekFromDate(dpFilter.getValue());
        LocalDate target = (weekStart == null ? LocalDate.now() : weekStart).minusWeeks(1);
        dpFilter.setValue(target);
    }

    @FXML
    private void nextWeek() {
        if (weekStart == null) updateWeekFromDate(dpFilter.getValue());
        LocalDate target = (weekStart == null ? LocalDate.now() : weekStart).plusWeeks(1);
        dpFilter.setValue(target);
    }

    @FXML
    private void createNew() {
        new Alert(Alert.AlertType.INFORMATION,
                "Pour créer un entretien, allez d'abord sur Candidatures puis cliquez sur 'Entretien' pour le candidat.")
                .show();

        try {
            Parent root = dpFilter.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof Pane pane)) return;

            var url = getClass().getResource("/candidaturefxml/RH_Candidatures.fxml");
            if (url == null) return;

            Parent view = FXMLLoader.load(url);
            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshWeekly() {
        if (weekGrid == null) return;

        candCache = new HashMap<>();
        weekGrid.getChildren().clear();
        weekGrid.getColumnConstraints().clear();
        weekGrid.getRowConstraints().clear();

        if (weekStart == null) {
            updateWeekFromDate(dpFilter.getValue());
        }

        // colonnes: 0 = heures, 1..7 = jours (Mon..Sun)
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(54);
        c0.setPrefWidth(54);
        weekGrid.getColumnConstraints().add(c0);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setMinWidth(0);
            cc.setPrefWidth(0);
            cc.setMaxWidth(Double.MAX_VALUE);
            weekGrid.getColumnConstraints().add(cc);
        }

        // Header (jours)
        weekGrid.add(new Label(""), 0, 0);
        DateTimeFormatter dowFmt = DateTimeFormatter.ofPattern("EEE", Locale.FRANCE);
        for (int d = 0; d < 7; d++) {
            LocalDate date = weekStart.plusDays(d);
            VBox head = new VBox(2);
            head.getStyleClass().add("rh-week-dayhead");
            if (dpFilter.getValue() != null && dpFilter.getValue().equals(date)) {
                head.getStyleClass().add("rh-week-dayhead-selected");
            }
            Label dow = new Label(date.format(dowFmt));
            dow.getStyleClass().add("rh-week-dow");
            Label dom = new Label(String.valueOf(date.getDayOfMonth()));
            dom.getStyleClass().add("rh-week-dom");
            head.getChildren().addAll(dow, dom);
            head.setOnMouseClicked(e -> dpFilter.setValue(date));
            weekGrid.add(head, d + 1, 0);
        }

        int rows = (END_HOUR - START_HOUR) + 1;

        for (int r = 0; r < rows; r++) {
            int hour = START_HOUR + r;
            Label time = new Label(String.format("%02d:00", hour));
            time.getStyleClass().add("rh-week-hour");
            weekGrid.add(time, 0, r + 1);

            for (int d = 0; d < 7; d++) {
                StackPane slot = new StackPane();
                slot.getStyleClass().add("rh-week-slot");
                slot.setAlignment(Pos.TOP_LEFT);
                slot.setMinHeight(58);
                slot.setPrefHeight(58);
                slot.setMaxWidth(Double.MAX_VALUE);
                weekGrid.add(slot, d + 1, r + 1);
            }
        }

        Integer managerId = (Session.getCurrentUser() != null) ? Session.getCurrentUser().getId() : null;
        List<Entretien> all = (managerId == null)
                ? java.util.Collections.emptyList()
                : entretienService.afficherEntretiensPourManager(managerId);
        Map<String, StackPane> slotIndex = new HashMap<>();
        weekGrid.getChildren().forEach(n -> {
            Integer col = GridPane.getColumnIndex(n);
            Integer row = GridPane.getRowIndex(n);
            if (col == null || row == null) return;
            if (col >= 1 && col <= 7 && row >= 1 && row <= rows) {
                if (n instanceof StackPane sp) {
                    slotIndex.put(col + ":" + row, sp);
                }
            }
        });

        int shown = 0;
        for (Entretien e : all) {
            if (e == null || e.getDate_heure() == null) continue;
            LocalDateTime dt = e.getDate_heure();
            LocalDate d = dt.toLocalDate();
            if (d.isBefore(weekStart) || d.isAfter(weekStart.plusDays(6))) continue;

            int col = dayToCol(d.getDayOfWeek());
            if (col < 1 || col > 7) continue;

            Integer row = slotRowFromDateTime(dt);
            if (row == null) continue;
            StackPane slot = slotIndex.get(col + ":" + row);
            if (slot == null) continue;

            VBox items;
            if (!slot.getChildren().isEmpty() && slot.getChildren().get(0) instanceof VBox vb) {
                items = vb;
            } else {
                items = new VBox(6);
                items.getStyleClass().add("rh-week-slot-items");
                slot.getChildren().setAll(items);
            }

            HBox chip = buildSlotChip(e);
            chip.setMaxHeight(Double.MAX_VALUE);
            chip.setAlignment(Pos.CENTER_LEFT);
            VBox.setVgrow(chip, Priority.ALWAYS);
            items.getChildren().add(chip);
            shown++;
        }

        if (shown == 0 && !autoJumpedToFirstEntretien && all != null && !all.isEmpty()) {
            try {
                Entretien first = all.get(0);
                if (first != null && first.getDate_heure() != null) {
                    autoJumpedToFirstEntretien = true;
                    dpFilter.setValue(first.getDate_heure().toLocalDate());
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        if (lblSummary != null) {
            lblSummary.setText(shown + " entretien(s)");
        }

        if (lblWeekRange != null) {
            String base = lblWeekRange.getText();
            if (base == null) base = "";
            if (!base.contains("entretien")) {
                lblWeekRange.setText(base + "  •  " + shown + " entretien(s)");
            }
        }
    }

    private HBox buildSlotChip(Entretien e) {
        HBox chip = new HBox(8);
        chip.getStyleClass().add("rh-week-chip");
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setMaxHeight(Double.MAX_VALUE);

        if (e != null && selectedEntretienId != null && e.getId_entretien() == selectedEntretienId) {
            chip.getStyleClass().add("rh-week-chip-selected");
        }

        String who = "Candidature #" + e.getId_candidature();
        try {
            Candidature c = getCandidature(e.getId_candidature());
            if (c != null) {
                String full = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
                if (!full.isBlank()) who = full;
            }
        } catch (Exception ignored) {
        }

        Label name = new Label(trunc(who, 22));
        name.getStyleClass().add("rh-week-chip-name");
        name.setMinWidth(0);
        HBox.setHgrow(name, Priority.ALWAYS);

        LocalTime t = e.getDate_heure().toLocalTime();
        Label time = new Label(String.format("%02d:%02d", t.getHour(), t.getMinute()));
        time.getStyleClass().add("rh-week-chip-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        chip.getChildren().addAll(name, spacer, time);

        String statut = safe(e.getStatut()).toUpperCase();
        if ("CONFIRME".equals(statut) || "TERMINE".equals(statut)) {
            chip.getStyleClass().add("rh-week-chip-green");
        } else {
            chip.getStyleClass().add("rh-week-chip-purple");
        }

        chip.setOnMouseClicked(ev -> {
            selectedEntretienId = e.getId_entretien();
            updateSelectedCard(e);
            refreshWeekly();
        });

        return chip;
    }

    private Integer slotRowFromDateTime(LocalDateTime dt) {
        if (dt == null) return null;

        int hour = dt.getHour();
        // place l'entretien dans la case de l'heure la plus proche (simplifié)
        if (dt.getMinute() > 30) hour++;

        // si l'entretien est hors plage, on l'affiche quand même (clamp)
        if (hour < START_HOUR) hour = START_HOUR;
        if (hour > END_HOUR) hour = END_HOUR;
        return (hour - START_HOUR) + 1;
    }

    private void openDetails(Entretien e) {
        if (e == null) return;
        try {
            Parent root = dpFilter.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof Pane pane)) {
                new Alert(Alert.AlertType.ERROR, "Zone d'affichage introuvable (adminContentArea).").show();
                return;
            }

            var url = getClass().getResource("/candidaturefxml/RH_EntretienDetails.fxml");
            if (url == null) {
                new Alert(Alert.AlertType.ERROR, "FXML introuvable: RH_EntretienDetails.fxml").show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienDetailsController ctrl = loader.getController();
            if (ctrl != null) {
                Entretien payload = e;
                try {
                    Entretien fresh = entretienService.getEntretienById(e.getId_entretien());
                    if (fresh != null) payload = fresh;
                } catch (Exception ignored) {
                }
                ctrl.setEntretien(payload);
            }

            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Candidature getCandidature(int id) {
        if (id <= 0) return null;
        if (candCache.containsKey(id)) return candCache.get(id);
        // mode hebdomadaire: on ne charge que si nécessaire via CandidatureService (lazy)
        CandidatureService candidatureService = new CandidatureService();
        Candidature c = candidatureService.getCandidatureById(id);
        candCache.put(id, c);
        return c;
    }

    private void openEdit(Entretien e) {
        try {
            Parent root = dpFilter.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof Pane pane)) {
                new Alert(Alert.AlertType.ERROR, "Zone d'affichage introuvable (adminContentArea).").show();
                return;
            }

            var url = getClass().getResource("/candidaturefxml/RH_EntretienForm.fxml");
            if (url == null) {
                new Alert(Alert.AlertType.ERROR, "FXML introuvable: RH_EntretienForm.fxml").show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienFormController ctrl = loader.getController();
            if (ctrl != null) {
                Entretien payload = e;
                try {
                    if (e != null) {
                        Entretien fresh = entretienService.getEntretienById(e.getId_entretien());
                        if (fresh != null) payload = fresh;
                    }
                } catch (Exception ignored) {
                }
                ctrl.openForEdit(payload);
            }

            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void delete(Entretien e) {
        if (e == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer cet entretien ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    entretienService.supprimerEntretien(e.getId_entretien());
                    if (selectedEntretienId != null && selectedEntretienId == e.getId_entretien()) {
                        selectedEntretienId = null;
                        updateSelectedCard(null);
                    }
                    refreshWeekly();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    private void updateCalendar() {
        if (lblMonth != null) {
            lblMonth.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }
        buildCalendarGrid();
    }

    private void buildCalendarGrid() {
        if (calendarGrid == null) return;

        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            calendarGrid.getColumnConstraints().add(cc);
        }

        YearMonth ym = currentMonth;
        LocalDate first = ym.atDay(1);
        int shift = dayOfWeekIndexMondayFirst(first.getDayOfWeek());
        LocalDate start = first.minusDays(shift);

        Map<LocalDate, Integer> counts = buildCountsForMonthGrid(start, 42);

        for (int cell = 0; cell < 42; cell++) {
            LocalDate date = start.plusDays(cell);
            int row = cell / 7;
            int col = cell % 7;

            Button b = new Button(String.valueOf(date.getDayOfMonth()));
            b.getStyleClass().add("rh-cal-day");
            if (!ym.equals(YearMonth.from(date))) {
                b.getStyleClass().add("rh-cal-day-muted");
            }
            if (dpFilter.getValue() != null && dpFilter.getValue().equals(date)) {
                b.getStyleClass().add("rh-cal-day-selected");
            }

            Integer c = counts.get(date);
            if (c != null && c > 0) {
                b.getStyleClass().add("rh-cal-day-has");
            }

            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> {
                dpFilter.setValue(date);
                updateCalendar();
            });
            calendarGrid.add(b, col, row);
        }
    }

    private Map<LocalDate, Integer> buildCountsForMonthGrid(LocalDate start, int days) {
        Map<LocalDate, Integer> map = new HashMap<>();

        List<Entretien> all;
        try {
            Integer managerId = (Session.getCurrentUser() != null) ? Session.getCurrentUser().getId() : null;
            all = (managerId == null)
                    ? java.util.Collections.emptyList()
                    : entretienService.afficherEntretiensPourManager(managerId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return map;
        }

        LocalDate end = start.plusDays(days);
        for (Entretien e : all) {
            if (e == null || e.getDate_heure() == null) continue;
            LocalDate d = e.getDate_heure().toLocalDate();
            if (d.isBefore(start) || !d.isBefore(end)) continue;
            map.put(d, map.getOrDefault(d, 0) + 1);
        }
        return map;
    }

    private int dayOfWeekIndexMondayFirst(DayOfWeek dow) {
        int v = dow.getValue();
        return (v == 7) ? 6 : (v - 1);
    }

    private void updateWeekFromDate(LocalDate date) {
        if (date == null) return;
        int idx = dayOfWeekIndexMondayFirst(date.getDayOfWeek());
        weekStart = date.minusDays(idx);
        if (lblWeekRange != null) {
            LocalDate today = LocalDate.now();
            lblWeekRange.setText(today.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    private int dayToCol(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
            default -> -1;
        };
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void updateSelectedCard(Entretien e) {
        boolean has = (e != null);
        if (!has && selectedEntretienId != null) {
            try {
                e = entretienService.getEntretienById(selectedEntretienId);
                has = (e != null);
            } catch (Exception ignored) {
            }
        }

        if (entretienDetailsBox != null) {
            entretienDetailsBox.setVisible(has);
            entretienDetailsBox.setManaged(has);
        }
        if (entretienPlaceholder != null) {
            entretienPlaceholder.setVisible(!has);
            entretienPlaceholder.setManaged(!has);
        }

        if (!has) {
            if (lblSelName != null) lblSelName.setText("Sélectionnez un entretien");
            if (lblSelSub != null) lblSelSub.setText("");
            if (lblSelTime != null) lblSelTime.setText("");
            if (btnSelDetails != null) btnSelDetails.setDisable(true);
            if (btnSelEdit != null) btnSelEdit.setDisable(true);
            if (btnSelDelete != null) btnSelDelete.setDisable(true);
            return;
        }

        String who = "Candidature #" + e.getId_candidature();
        try {
            Candidature c = getCandidature(e.getId_candidature());
            if (c != null) {
                String full = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
                if (!full.isBlank()) who = full;
            }
        } catch (Exception ignored) {
        }

        if (lblSelName != null) lblSelName.setText(who);
        if (lblSelSub != null) {
            String mode = safe(e.getMode());
            String extra = "";
            if ("En ligne".equalsIgnoreCase(mode)) {
                extra = safe(e.getPlatform());
            } else {
                extra = safe(e.getLieu());
            }
            lblSelSub.setText(mode + (extra.isBlank() ? "" : (" • " + extra)));
        }
        if (lblSelTime != null && e.getDate_heure() != null) {
            LocalTime t = e.getDate_heure().toLocalTime();
            lblSelTime.setText(String.format("%02d:%02d", t.getHour(), t.getMinute()) + "  -  " + String.format("%02d:%02d", (t.getHour() + 1) % 24, t.getMinute()));
        }

        if (btnSelDetails != null) btnSelDetails.setDisable(false);
        if (btnSelEdit != null) btnSelEdit.setDisable(false);
        if (btnSelDelete != null) btnSelDelete.setDisable(false);
    }

    @FXML
    private void openSelectedDetails() {
        if (selectedEntretienId == null) {
            new Alert(Alert.AlertType.INFORMATION, "Sélectionnez un entretien d'abord (clique sur un nom dans la grille).").show();
            return;
        }
        try {
            Entretien e = entretienService.getEntretienById(selectedEntretienId);
            if (e == null) return;
            openDetails(e);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les détails.").show();
        }
    }

    @FXML
    private void editSelected() {
        if (selectedEntretienId == null) {
            new Alert(Alert.AlertType.INFORMATION, "Sélectionnez un entretien d'abord (clique sur un nom dans la grille).").show();
            return;
        }
        try {
            Entretien e = entretienService.getEntretienById(selectedEntretienId);
            if (e == null) return;
            openEdit(e);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire.").show();
        }
    }

    @FXML
    private void deleteSelected() {
        if (selectedEntretienId == null) {
            new Alert(Alert.AlertType.INFORMATION, "Sélectionnez un entretien d'abord (clique sur un nom dans la grille).").show();
            return;
        }
        try {
            Entretien e = entretienService.getEntretienById(selectedEntretienId);
            if (e == null) return;
            delete(e);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de supprimer l'entretien.").show();
        }
    }

    private String trunc(String s, int n) {
        String x = safe(s);
        if (x.length() <= n) return x;
        return x.substring(0, Math.max(0, n - 1)) + "…";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
