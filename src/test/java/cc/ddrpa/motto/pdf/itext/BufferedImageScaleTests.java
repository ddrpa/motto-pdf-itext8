package cc.ddrpa.motto.pdf.itext;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class BufferedImageScaleTests {

    private BufferedImage scaleImage(BufferedImage before, int targetWidth, int targetHeight) {
        int originalWidth = before.getWidth();
        int originalHeight = before.getHeight();
        BufferedImage after = new BufferedImage(targetWidth, targetHeight, before.getType());
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(
            1.0 * targetWidth / originalWidth, 1.0 * targetHeight / originalHeight);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleTransform,
            AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(before, after);
    }

    @Test
    void scaleTest() throws IOException {
        BufferedImage original = ImageIO.read(new File("src/test/resources/large-photo.jpeg"));
        BufferedImage resized = scaleImage(original, 100, 200);
        ImageIO.write(resized, "jpg", new File("target/resized.jpg"));
    }
}