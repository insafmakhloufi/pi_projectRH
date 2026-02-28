package Services.Formation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import Entities.Formation.Chapitre;
import Entities.Formation.Cour;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MlComplexityPredictor {

    private static final String MODEL_PATH = "/ml/complexity_model.onnx";
    private static final String SCHEMA_PATH = "/ml/feature_schema.json";

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    private static int countPdfFilesFromContenu(String contenu) {
        if (contenu == null) {
            return 0;
        }
        String v = contenu.trim();
        if (v.isEmpty()) {
            return 0;
        }
        String lower = v.toLowerCase();
        if (lower.startsWith(PDF_PREFIX)) {
            String raw = v.substring(PDF_PREFIX.length()).trim();
            return raw.isEmpty() ? 0 : 1;
        }
        if (lower.startsWith(PDFS_PREFIX)) {
            String raw = v.substring(PDFS_PREFIX.length()).trim();
            if (raw.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (String p : raw.split(";")) {
                if (p == null) continue;
                if (!p.trim().isEmpty()) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }

    private final OrtEnvironment env;
    private final OrtSession session;
    private final Schema schema;

    private static final class Schema {
        String input_name;
        String[] features;
        String[] classes;
    }

    public MlComplexityPredictor() {
        OrtEnvironment tmpEnv = null;
        OrtSession tmpSession = null;
        Schema tmpSchema = null;
        try {
            tmpSchema = loadSchema();
            tmpEnv = OrtEnvironment.getEnvironment();
            tmpSession = tmpEnv.createSession(loadResourceBytes(MODEL_PATH), new OrtSession.SessionOptions());
        } catch (Exception ignored) {
            tmpEnv = null;
            tmpSession = null;
            tmpSchema = null;
        }
        this.env = tmpEnv;
        this.session = tmpSession;
        this.schema = tmpSchema;
    }

    public boolean isLoaded() {
        return env != null && session != null && schema != null;
    }

    public String predictLabel(Cour cour, List<Chapitre> chapitres) {
        if (cour == null) {
            return "Analyse impossible : Cours null";
        }

        if (!isLoaded()) {
            throw new IllegalStateException("ML model is not loaded");
        }

        float[] features = computeFeatures(cour, chapitres);

        try (OnnxTensor input = OnnxTensor.createTensor(env, new float[][] { features })) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(getInputName(), input);

            try (Result result = session.run(inputs)) {
                String label = extractLabel(result);
                if (label != null && !label.isBlank()) {
                    return label;
                }
            }
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("ML prediction failed");
    }

    private String getInputName() {
        if (schema != null && schema.input_name != null && !schema.input_name.isBlank()) {
            return schema.input_name;
        }
        return "input";
    }

    private float[] computeFeatures(Cour cour, List<Chapitre> chapitres) {
        int nbChapters = 0;
        int pdfCount = 0;
        int textCount = 0;
        long totalTextLen = 0;

        if (chapitres != null) {
            nbChapters = chapitres.size();
            for (Chapitre ch : chapitres) {
                if (ch == null) continue;
                String contenu = ch.getContenu();
                if (contenu == null) continue;
                int pdfFiles = countPdfFilesFromContenu(contenu);
                if (pdfFiles > 0) {
                    pdfCount += pdfFiles;
                } else {
                    long len = contenu.trim().length();
                    if (len > 0) {
                        textCount++;
                        totalTextLen += len;
                    }
                }
            }
        } else {
            nbChapters = Math.max(0, cour.getNbChapitres());
        }

        double avgTextLen = (textCount == 0) ? 0.0 : ((double) totalTextLen / textCount);
        double estimatedTextMinutes = (double) totalTextLen / 900.0;
        double estimatedPdfMinutes = pdfCount * 20.0;
        double estimatedTotalMinutes = estimatedTextMinutes + estimatedPdfMinutes;

        Map<String, Float> f = new HashMap<>();
        f.put("nb_chapters", (float) nbChapters);
        f.put("pdf_count", (float) pdfCount);
        f.put("text_count", (float) textCount);
        f.put("total_text_len", (float) totalTextLen);
        f.put("avg_text_len", (float) avgTextLen);
        f.put("estimated_text_minutes", (float) estimatedTextMinutes);
        f.put("estimated_total_minutes", (float) estimatedTotalMinutes);

        String[] order = (schema != null && schema.features != null && schema.features.length > 0)
                ? schema.features
                : new String[] { "nb_chapters", "pdf_count", "text_count", "total_text_len", "avg_text_len", "estimated_text_minutes", "estimated_total_minutes" };

        float[] out = new float[order.length];
        for (int i = 0; i < order.length; i++) {
            out[i] = f.getOrDefault(order[i], 0.0f);
        }
        return out;
    }

    private String extractLabel(Result result) {
        if (result == null) return null;

        for (Map.Entry<String, OnnxValue> entry : result) {
            OnnxValue v = entry == null ? null : entry.getValue();
            if (v == null) continue;
            Object value;
            try {
                value = v.getValue();
            } catch (OrtException e) {
                continue;
            }
            if (value instanceof String[]) {
                String[] arr = (String[]) value;
                if (arr.length > 0) return arr[0];
            }
            if (value instanceof String) {
                return (String) value;
            }
            if (value instanceof long[]) {
                long[] arr = (long[]) value;
                if (arr.length > 0 && schema != null && schema.classes != null && arr[0] >= 0 && arr[0] < schema.classes.length) {
                    return schema.classes[(int) arr[0]];
                }
            }
            if (value instanceof int[]) {
                int[] arr = (int[]) value;
                if (arr.length > 0 && schema != null && schema.classes != null && arr[0] >= 0 && arr[0] < schema.classes.length) {
                    return schema.classes[arr[0]];
                }
            }
        }

        return null;
    }

    private Schema loadSchema() throws IOException {
        String json = new String(loadResourceBytes(SCHEMA_PATH), StandardCharsets.UTF_8);
        Gson gson = new Gson();

        JsonObject obj = gson.fromJson(json, JsonObject.class);
        Schema s = new Schema();
        s.input_name = obj.has("input_name") ? obj.get("input_name").getAsString() : "input";
        if (obj.has("features")) {
            s.features = gson.fromJson(obj.get("features"), String[].class);
        }
        if (obj.has("classes")) {
            s.classes = gson.fromJson(obj.get("classes"), String[].class);
        }
        return s;
    }

    private byte[] loadResourceBytes(String path) throws IOException {
        try (InputStream in = MlComplexityPredictor.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Resource not found: " + path);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        }
    }
}
