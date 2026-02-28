package Services.User;

import Utils.Mydatabase;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;

public class FaceAuthService {

    private final Connection conn;

    public FaceAuthService() {
        this(Mydatabase.getInstance().getConnection());
    }

    public FaceAuthService(Connection conn) {
        this.conn = conn;
        ensureTable();
    }

    public boolean isEnabled(int userId) {
        String sql = "SELECT enabled FROM user_face_auth WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enabled") == 1;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public void enable(int userId) {
        String sql = "UPDATE user_face_auth SET enabled=1 WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void disable(int userId) {
        String sql = "UPDATE user_face_auth SET enabled=0 WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasModel(int userId) {
        String sql = "SELECT model_xml FROM user_face_auth WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("model_xml");
                    return bytes != null && bytes.length > 0;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public void saveModel(int userId, LBPHFaceRecognizer recognizer) {
        if (recognizer == null) {
            throw new IllegalArgumentException("Recognizer is required");
        }

        byte[] modelBytes = serializeRecognizer(recognizer);
        String sql = "INSERT INTO user_face_auth (user_id, model_xml, enabled) VALUES (?,?,0) " +
                "ON DUPLICATE KEY UPDATE model_xml=VALUES(model_xml)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setBytes(2, modelBytes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LBPHFaceRecognizer loadRecognizer(int userId) {
        String sql = "SELECT model_xml FROM user_face_auth WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] bytes = rs.getBytes("model_xml");
                if (bytes == null || bytes.length == 0) return null;
                return deserializeRecognizer(bytes);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verify(int userId, Mat faceGray) {
        if (faceGray == null || faceGray.empty()) return false;

        LBPHFaceRecognizer recognizer = loadRecognizer(userId);
        if (recognizer == null) return false;

        int[] predictedLabel = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(faceGray, predictedLabel, confidence);

        // For per-user model, we expect label = 1. Lower confidence is better.
        return predictedLabel[0] == 1 && confidence[0] >= 0 && confidence[0] < 80;
    }

    /**
     * Find a user by face match across all registered and enabled face models.
     * Returns the userId of the best match, or -1 if no match found.
     */
    public int findUserByFaceMatch(Mat faceGray) {
        if (faceGray == null || faceGray.empty()) return -1;

        String sql = "SELECT user_id FROM user_face_auth WHERE enabled = 1 AND model_xml IS NOT NULL";
        int bestUserId = -1;
        double bestConfidence = Double.MAX_VALUE;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                LBPHFaceRecognizer recognizer = loadRecognizer(userId);
                if (recognizer == null) continue;

                int[] predictedLabel = new int[1];
                double[] confidence = new double[1];
                recognizer.predict(faceGray, predictedLabel, confidence);

                // Check if this is a valid match (label = 1, confidence < threshold)
                if (predictedLabel[0] == 1 && confidence[0] >= 0 && confidence[0] < 80) {
                    // Lower confidence is better (more confident match)
                    if (confidence[0] < bestConfidence) {
                        bestConfidence = confidence[0];
                        bestUserId = userId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[FaceAuthService] Error finding user by face match: " + e.getMessage());
        }

        if (bestUserId != -1) {
            System.out.println("[FaceAuthService] Face match found for user " + bestUserId + " with confidence " + bestConfidence);
        }
        return bestUserId;
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_face_auth (" +
                "user_id INT PRIMARY KEY," +
                "model_xml LONGBLOB NULL," +
                "enabled TINYINT NOT NULL DEFAULT 0," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_user_face_auth_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    private static byte[] serializeRecognizer(LBPHFaceRecognizer recognizer) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("careerlink-face-", ".xml");
            recognizer.write(tmp.toAbsolutePath().toString());
            byte[] rawBytes = Files.readAllBytes(tmp);
            // Compress with GZIP to reduce size for database storage
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(rawBytes);
            }
            byte[] compressed = baos.toByteArray();
            System.out.println("[FaceAuthService] Model size: raw=" + rawBytes.length + " bytes, compressed=" + compressed.length + " bytes");
            return compressed;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static LBPHFaceRecognizer deserializeRecognizer(byte[] compressedBytes) {
        Path tmp = null;
        try {
            // Decompress GZIP data
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = gzip.read(buffer)) > 0) {
                    baos.write(buffer, 0, read);
                }
            }
            byte[] xmlBytes = baos.toByteArray();
            tmp = Files.createTempFile("careerlink-face-", ".xml");
            Files.write(tmp, xmlBytes);
            LBPHFaceRecognizer r = LBPHFaceRecognizer.create();
            r.read(tmp.toAbsolutePath().toString());
            return r;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static LBPHFaceRecognizer trainPerUser(MatVector facesGray, Mat labels) {
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.train(facesGray, labels);
        return recognizer;
    }

    public static Mat buildLabels(int count) {
        Mat labels = new Mat(count, 1, CV_32SC1);
        // label all samples as 1
        try (IntIndexer idx = labels.createIndexer()) {
            for (int i = 0; i < count; i++) {
                idx.put(i, 0, 1);
            }
        }
        return labels;
    }
}
