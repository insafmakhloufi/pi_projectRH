package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenCvCascadeLoader {

    private OpenCvCascadeLoader() {
    }

    public static String loadFrontalFaceCascadePath() {
        // 1) Prefer classpath resource so the project works for everyone via git pull
        String[] classpathCandidates = {
                "/User/opencv/haarcascade_frontalface_default.xml",
                "/opencv/haarcascade_frontalface_default.xml",
                "/haarcascade_frontalface_default.xml"
        };

        for (String classpathResource : classpathCandidates) {
            System.out.println("[OpenCvCascadeLoader] Trying resource: " + classpathResource);
            InputStream in = OpenCvCascadeLoader.class.getResourceAsStream(classpathResource);
            if (in != null) {
                try {
                    Path tmp = Files.createTempFile("careerlink-cascade-", ".xml");
                    // Explicitly copy with buffer flush
                    try (java.io.OutputStream out = Files.newOutputStream(tmp)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                    }
                    in.close();
                    
                    tmp.toFile().deleteOnExit();
                    String result = tmp.toAbsolutePath().toString();
                    long size = Files.size(tmp);
                    System.out.println("[OpenCvCascadeLoader] Extracted cascade to: " + result + " (size: " + size + " bytes)");
                    
                    // Verify file exists and has substantial content (should be ~1MB)
                    if (Files.exists(tmp) && size > 100000) {
                        return result;
                    } else {
                        System.err.println("[OpenCvCascadeLoader] File too small or doesn't exist: " + size);
                    }
                } catch (IOException e) {
                    System.err.println("[OpenCvCascadeLoader] Error extracting resource " + classpathResource + ": " + e.getMessage());
                    try {
                        in.close();
                    } catch (IOException ignored) {}
                }
            } else {
                System.out.println("[OpenCvCascadeLoader] Resource not found: " + classpathResource);
            }
        }

        // 2) Fallback to env var for local override
        String env = System.getenv("OPENCV_HAAR_CASCADE_PATH");
        if (env != null && !env.isBlank()) {
            System.out.println("[OpenCvCascadeLoader] Using env var: " + env);
            return env;
        }

        System.err.println("[OpenCvCascadeLoader] Could not load Haar cascade from any source!");
        return null;
    }
}
