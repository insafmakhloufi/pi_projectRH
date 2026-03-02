package Services.candidature;



import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CameraService
 * Capture les frames de la webcam via FFmpeg et les affiche dans un ImageView JavaFX.
 *
 * Fonctionnement :
 *   FFmpeg capture la webcam → envoie des frames JPEG sur stdout
 *   Un thread Java lit ces frames en continu → les affiche dans l'ImageView
 */
public class Cameraservice {

    // ── Config ──────────────────────────────────────────────────────────────
    private static final int FRAME_WIDTH  = 640;
    private static final int FRAME_HEIGHT = 480;
    private static final int FPS          = 15;

    // Marqueur de début d'une image JPEG (magic bytes)
    private static final byte[] JPEG_START = {(byte) 0xFF, (byte) 0xD8};
    // Marqueur de fin d'une image JPEG
    private static final byte[] JPEG_END   = {(byte) 0xFF, (byte) 0xD9};

    // ── État interne ─────────────────────────────────────────────────────────
    private Process         ffmpegProcess;
    private InputStream     ffmpegOutput;
    private Thread          readerThread;
    private Thread          stderrThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastError;

    private final ImageView cameraView; // L'ImageView JavaFX à mettre à jour

    // ── Constructeur ─────────────────────────────────────────────────────────
    public Cameraservice(ImageView cameraView) {
        this.cameraView = cameraView;
    }

    // ── Démarrer la caméra ───────────────────────────────────────────────────
    public void startCamera() {
        if (running.get()) return; // déjà démarrée

        try {
            lastError = null;
            String ffmpegExe = resolveFfmpegExecutable();
            ensureFfmpegAvailable(ffmpegExe);

            String forcedDevice = getForcedCameraDeviceName().orElse(null);
            String deviceName = (forcedDevice != null && !forcedDevice.isBlank())
                    ? forcedDevice
                    : detectDefaultVideoDevice(ffmpegExe).orElse(null);
            boolean useDefault = (deviceName == null || deviceName.isBlank());
            String dshowInput = useDefault ? "video=default" : ("video=\"" + deviceName + "\"");

            // Commande FFmpeg :
            // -f dshow         → DirectShow (Windows) pour capturer la webcam
            // -i video="..."   → nom du périphérique vidéo (auto-détecté ici)
            // -vf scale        → redimensionner
            // -r               → framerate
            // -f image2pipe    → envoyer frames sur stdout
            // -vcodec mjpeg    → format JPEG
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExe,
                    "-hide_banner",
                    "-f", "dshow",
                    "-i", dshowInput,
                    "-vf", "scale=" + FRAME_WIDTH + ":" + FRAME_HEIGHT +
                    ",hflip",                // hflip = effet miroir (naturel pour se voir)
                    "-r", String.valueOf(FPS),
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-q:v", "5",                    // qualité JPEG (1=max, 31=min)
                    "pipe:1"                        // sortie sur stdout
            );

            pb.redirectErrorStream(false);
            ffmpegProcess = pb.start();
            startStderrConsumerThread(ffmpegProcess.getErrorStream());
            ffmpegOutput  = new BufferedInputStream(ffmpegProcess.getInputStream(), 1024 * 512);

            running.set(true);

            // Thread dédié à la lecture des frames
            readerThread = new Thread(this::readFramesLoop, "CameraReader");
            readerThread.setDaemon(true);
            readerThread.start();

            System.out.println("[Cameraservice] Caméra démarrée.");

        } catch (Exception e) {
            lastError = e.getMessage();
            System.err.println("[Cameraservice] Erreur démarrage : " + e.getMessage());
            System.err.println("[Cameraservice] Vérifie : (1) FFmpeg installé (2) Autorisation caméra Windows (3) Webcam non utilisée par une autre app");
        }
    }

    public String getLastError() {
        return lastError;
    }

    private void startStderrConsumerThread(InputStream err) {
        if (err == null) return;
        stderrThread = new Thread(() -> {
            Charset cs = Charset.defaultCharset();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(err, cs))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Important: consommer stderr pour éviter le blocage du process.
                    System.err.println("[Cameraservice:ffmpeg] " + line);
                }
            } catch (Exception ignored) {
            }
        }, "CameraStderrReader");
        stderrThread.setDaemon(true);
        stderrThread.start();
    }

    private String resolveFfmpegExecutable() {
        // 0) Override explicite
        String override = System.getProperty("ffmpeg.path");
        if (override == null || override.isBlank()) override = System.getenv("FFMPEG_PATH");
        if (override != null && !override.isBlank()) return override;

        // 0.1) Chemins relatifs au projet (quand tu mets ffmpeg dans le repo)
        String cwd = System.getProperty("user.dir", "");
        if (!cwd.isBlank()) {
            List<Path> rel = List.of(
                    Paths.get(cwd, "ffmpeg", "bin", "ffmpeg.exe"),
                    Paths.get(cwd, "ffmpeg", "ffmpeg.exe"),
                    Paths.get(cwd, "tools", "ffmpeg", "bin", "ffmpeg.exe"),
                    Paths.get(cwd, "tools", "ffmpeg", "ffmpeg.exe")
            );
            for (Path p : rel) {
                if (Files.exists(p)) return p.toString();
            }
        }

        // 1) PATH
        String fromPath = findOnPath("ffmpeg.exe").orElse(null);
        if (fromPath != null) return fromPath;

        // 1.1) Downloads (ex: ffmpeg-8.0.1-essentials_build/.../bin/ffmpeg.exe)
        String downloads = findFfmpegInDownloads().orElse(null);
        if (downloads != null) return downloads;

        // 2) Emplacements courants
        String userHome = System.getProperty("user.home", "");
        List<Path> candidates = List.of(
                Paths.get("C:/ffmpeg/bin/ffmpeg.exe"),
                Paths.get("C:/ffmpeg/ffmpeg.exe"),
                Paths.get(userHome, "ffmpeg", "bin", "ffmpeg.exe"),
                Paths.get(userHome, "ffmpeg", "ffmpeg.exe"),
                Paths.get(userHome, "Downloads", "ffmpeg", "bin", "ffmpeg.exe"),
                Paths.get(userHome, "Downloads", "ffmpeg", "ffmpeg.exe"),
                Paths.get("C:/Users/msi/ffmpeg/bin/ffmpeg.exe"),
                Paths.get("C:/Users/msi/ffmpeg/ffmpeg.exe")
        );
        for (Path p : candidates) {
            if (Files.exists(p)) return p.toString();
        }

        // 3) Fallback: essayer "ffmpeg" (peut marcher si PATH est configuré différemment)
        return "ffmpeg";
    }

    private Optional<String> findFfmpegInDownloads() {
        try {
            String userHome = System.getProperty("user.home", "");
            if (userHome.isBlank()) return Optional.empty();
            Path downloads = Paths.get(userHome, "Downloads");
            if (!Files.isDirectory(downloads)) return Optional.empty();

            // Cherche un dossier qui commence par "ffmpeg" puis tente des chemins connus.
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(downloads, "ffmpeg*")) {
                for (Path root : ds) {
                    if (!Files.isDirectory(root)) continue;

                    // Cas 1: .../<root>/bin/ffmpeg.exe
                    Path p1 = root.resolve("bin").resolve("ffmpeg.exe");
                    if (Files.exists(p1)) return Optional.of(p1.toString());

                    // Cas 2: .../<root>/<root>/bin/ffmpeg.exe (comme certains zips)
                    Path inner = root.resolve(root.getFileName().toString());
                    Path p2 = inner.resolve("bin").resolve("ffmpeg.exe");
                    if (Files.exists(p2)) return Optional.of(p2.toString());

                    // Cas 3: .../<root>/**/bin/ffmpeg.exe (1 niveau)
                    try (DirectoryStream<Path> ds2 = Files.newDirectoryStream(root)) {
                        for (Path child : ds2) {
                            if (!Files.isDirectory(child)) continue;
                            Path p3 = child.resolve("bin").resolve("ffmpeg.exe");
                            if (Files.exists(p3)) return Optional.of(p3.toString());

                            Path p4 = child.resolve(child.getFileName().toString()).resolve("bin").resolve("ffmpeg.exe");
                            if (Files.exists(p4)) return Optional.of(p4.toString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // Fallback direct: chemin le plus courant pour ton cas
            Path known = Paths.get(userHome, "Downloads", "ffmpeg-8.0.1-essentials_build", "ffmpeg-8.0.1-essentials_build", "bin", "ffmpeg.exe");
            if (Files.exists(known)) return Optional.of(known.toString());

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> findOnPath(String exeName) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) return Optional.empty();
        String[] parts = path.split(File.pathSeparator);
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            Path candidate = Paths.get(part, exeName);
            if (Files.exists(candidate)) return Optional.of(candidate.toString());
        }
        return Optional.empty();
    }

    private Optional<String> detectDefaultVideoDevice(String ffmpegExe) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExe,
                    "-hide_banner",
                    "-list_devices", "true",
                    "-f", "dshow",
                    "-i", "dummy"
            );
            pb.redirectErrorStream(false);
            Process p = pb.start();

            List<String> devices = new ArrayList<>();
            List<String> raw = new ArrayList<>();
            Charset cs = Charset.defaultCharset();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), cs))) {
                boolean inVideoSection = false;
                String pendingDisplayVideoName = null;
                String line;
                while ((line = br.readLine()) != null) {
                    raw.add(line);
                    String lower = line.toLowerCase();

                    if (lower.contains("(video)")) {
                        int q1 = line.indexOf('"');
                        int q2 = (q1 >= 0) ? line.indexOf('"', q1 + 1) : -1;
                        if (q1 >= 0 && q2 > q1) {
                            String name = line.substring(q1 + 1, q2).trim();
                            if (!name.isBlank()) {
                                pendingDisplayVideoName = name;
                                devices.add(name);
                            }
                        }
                        continue;
                    }

                    if (pendingDisplayVideoName != null && lower.contains("alternative name")) {
                        int q1 = line.indexOf('"');
                        int q2 = (q1 >= 0) ? line.indexOf('"', q1 + 1) : -1;
                        if (q1 >= 0 && q2 > q1) {
                            String alt = line.substring(q1 + 1, q2).trim();
                            if (!alt.isBlank()) {
                                // Préférer l'alternative name (souvent sans espaces)
                                return Optional.of(alt);
                            }
                        }
                        pendingDisplayVideoName = null;
                        continue;
                    }

                    if (lower.contains("directshow") && lower.contains("video") && lower.contains("devices")) {
                        inVideoSection = true;
                        continue;
                    }
                    if (inVideoSection && lower.contains("directshow") && lower.contains("audio") && lower.contains("devices")) {
                        break;
                    }
                    if (!inVideoSection) continue;

                    // Exemple:
                    // [dshow @ ...]  "Integrated Camera"
                    // [dshow @ ...]     Alternative name "@device_pnp_..."
                    int q1 = line.indexOf('"');
                    int q2 = (q1 >= 0) ? line.indexOf('"', q1 + 1) : -1;
                    if (q1 >= 0 && q2 > q1) {
                        String name = line.substring(q1 + 1, q2).trim();
                        if (!name.isBlank() && !lower.contains("alternative name")) {
                            devices.add(name);
                            pendingDisplayVideoName = name;
                        }
                    }
                }
            }

            try {
                p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            p.destroy();

            if (devices.isEmpty()) {
                System.err.println("[Cameraservice] FFmpeg n'a retourné aucun device video (dshow). Sortie complète:");
                for (String l : raw) {
                    System.err.println("[Cameraservice:ffmpeg:list_devices] " + l);
                }
            }

            return devices.stream().findFirst();
        } catch (Exception e) {
            System.err.println("[Cameraservice] Detection device échouée: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> getForcedCameraDeviceName() {
        String p = System.getProperty("camera.dshow.name");
        if (p != null && !p.isBlank()) return Optional.of(p);
        String e = System.getenv("CAMERA_DSHOW_NAME");
        if (e != null && !e.isBlank()) return Optional.of(e);
        return Optional.empty();
    }

    private void ensureFfmpegAvailable(String ffmpegExe) {
        try {
            Process p = new ProcessBuilder(ffmpegExe, "-version").start();
            try {
                p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            p.destroy();
        } catch (Exception e) {
            throw new RuntimeException("FFmpeg introuvable/non exécutable: " + ffmpegExe);
        }
    }

    // ── Boucle de lecture des frames JPEG ────────────────────────────────────
    private void readFramesLoop() {
        /*
         * FFmpeg envoie un flux MJPEG continu sur stdout.
         * Chaque frame est un JPEG complet : commence par FF D8, finit par FF D9.
         * On lit byte par byte pour détecter ces marqueurs.
         */
        ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream(1024 * 100);

        try {
            int b;
            int prev = -1;

            while (running.get() && (b = ffmpegOutput.read()) != -1) {

                frameBuffer.write(b);

                // Détecter la fin d'une frame JPEG (FF D9)
                if (prev == 0xFF && b == 0xD9) {
                    byte[] jpegBytes = frameBuffer.toByteArray();
                    frameBuffer.reset();

                    // Afficher la frame dans le JavaFX thread
                    final byte[] frameCopy = jpegBytes;
                    Platform.runLater(() -> {
                        try {
                            Image img = new Image(
                                    new java.io.ByteArrayInputStream(frameCopy)
                            );
                            cameraView.setImage(img);
                        } catch (Exception ignored) {}
                    });
                }

                prev = b;
            }
        } catch (Exception e) {
            if (running.get()) {
                System.err.println("[Cameraservice] Erreur lecture frame : " + e.getMessage());
            }
        }

        System.out.println("[Cameraservice] Boucle de lecture terminée.");
    }

    // ── Arrêter la caméra ────────────────────────────────────────────────────
    public void stopCamera() {
        running.set(false);

        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
            System.out.println("[Cameraservice] FFmpeg arrêté.");
        }

        if (readerThread != null) {
            readerThread.interrupt();
        }

        if (stderrThread != null) {
            stderrThread.interrupt();
        }
    }

    // ── Vérifier si la caméra tourne ─────────────────────────────────────────
    public boolean isRunning() {
        return running.get();
    }
}
