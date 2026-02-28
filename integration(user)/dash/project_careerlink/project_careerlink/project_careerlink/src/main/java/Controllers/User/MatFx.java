package Controllers.User;

import javafx.scene.image.Image;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.ByteArrayInputStream;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;

public class MatFx {

    private MatFx() {
    }

    public static Image toImage(Mat bgrMat) {
        if (bgrMat == null || bgrMat.empty()) return null;
        BytePointer buf = new BytePointer();
        try {
            imencode(".jpg", bgrMat, buf);
            long size = buf.limit();
            if (size <= 0) return null;
            byte[] bytes = new byte[(int) size];
            buf.get(bytes);
            return new Image(new ByteArrayInputStream(bytes));
        } finally {
            try {
                buf.deallocate();
            } catch (Exception ignored) {
            }
        }
    }
}
