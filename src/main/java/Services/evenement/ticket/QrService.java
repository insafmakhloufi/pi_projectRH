package Services.evenement.ticket;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class QrService {

    public Path generateQrPng(String url, Path outputPath) throws Exception {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("url is required");
        if (outputPath == null) throw new IllegalArgumentException("outputPath is required");

        Path parent = outputPath.getParent();
        if (parent != null) Files.createDirectories(parent);

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter().encode(
                url,
                BarcodeFormat.QR_CODE,
                420,
                420,
                hints
        );

        MatrixToImageWriter.writeToPath(matrix, "PNG", outputPath);
        return outputPath;
    }
}