package Services.Export;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de génération de QR Codes
 * Permet d'accéder rapidement aux offres via mobile
 */
public class QrCodeService {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    /**
     * Génère un QR Code pour une offre d'emploi
     * @param offerId ID de l'offre
     * @param baseUrl URL de base de l'application (ex: http://localhost:8080)
     * @param outputFile Fichier de sortie
     * @throws IOException Si erreur d'écriture
     */
    public void generateOfferQrCode(int offerId, String baseUrl, File outputFile) throws IOException {
        // URL complète pour accéder à l'offre
        String url = baseUrl + "/offre/" + offerId;
        generateQrCode(url, outputFile, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Génère un QR Code avec texte personnalisé
     * @param text Texte à encoder
     * @param outputFile Fichier de sortie
     * @param width Largeur en pixels
     * @param height Hauteur en pixels
     * @throws IOException Si erreur d'écriture
     */
    public void generateQrCode(String text, File outputFile, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Configuration pour meilleure qualité
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Sauvegarder en PNG
            ImageIO.write(qrImage, "PNG", outputFile);

        } catch (WriterException e) {
            throw new IOException("Erreur génération QR Code: " + e.getMessage(), e);
        }
    }

    /**
     * Génère un QR Code avec logo CareerLink (version simplifiée)
     * @param offerId ID de l'offre
     * @param baseUrl URL de base
     * @param outputFile Fichier de sortie
     * @throws IOException Si erreur
     */
    public void generateStyledQrCode(int offerId, String baseUrl, File outputFile) throws IOException {
        // Pour l'instant, même chose que generateOfferQrCode
        // Future amélioration: ajouter un logo au centre du QR
        generateOfferQrCode(offerId, baseUrl, outputFile);
    }

    /**
     * Retourne une BufferedImage du QR Code (pour intégration UI)
     * @param text Texte à encoder
     * @param width Largeur
     * @param height Hauteur
     * @return BufferedImage du QR Code
     * @throws IOException Si erreur
     */
    public BufferedImage generateQrCodeImage(String text, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (WriterException e) {
            throw new IOException("Erreur génération QR Code: " + e.getMessage(), e);
        }
    }
}
