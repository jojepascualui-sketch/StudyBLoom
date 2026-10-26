import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * Draws the StudyBloom five-petal flower (from the design reference) into the
 * PWA icons. Run once from the project root:
 *
 *   java tools/IconGenerator.java
 *
 * Writes src/main/webapp/icons/*.png and favicon.svg.
 */
public class IconGenerator {
    private static final Color BLUSH = new Color(0xFFF0F5);
    private static final Color PETAL = new Color(0xF472B6);
    private static final Color PETAL_LIGHT = new Color(0xF9A8D4);
    private static final Color ROSE = new Color(0xBE185D);

    public static void main(String[] args) throws IOException {
        File dir = new File("src/main/webapp/icons");
        dir.mkdirs();
        // "any" icons: flower fills most of the square on a blush rounded tile.
        write(draw(192, 0.92, true), new File(dir, "icon-192.png"));
        write(draw(512, 0.92, true), new File(dir, "icon-512.png"));
        // "maskable" icons: full-bleed background, flower inside the 80% safe zone.
        write(draw(192, 0.62, false), new File(dir, "maskable-192.png"));
        write(draw(512, 0.62, false), new File(dir, "maskable-512.png"));
        write(draw(180, 0.70, false), new File(dir, "apple-touch-icon.png"));
        Files.writeString(new File(dir, "favicon.svg").toPath(), FAVICON);
        System.out.println("Icons written to " + dir.getPath());
    }

    /** scale = how much of the canvas the 100-unit flower drawing covers. */
    private static BufferedImage draw(int size, double scale, boolean roundedTile) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(BLUSH);
        if (roundedTile) {
            int r = size / 4;
            g.fillRoundRect(0, 0, size, size, r, r);
        } else {
            g.fillRect(0, 0, size, size);
        }
        double unit = size * scale / 100.0;
        g.translate(size / 2.0, size / 2.0);
        g.scale(unit, unit);
        Color[] colors = {PETAL, PETAL_LIGHT, PETAL, PETAL_LIGHT, PETAL};
        for (int i = 0; i < 5; i++) {
            AffineTransform saved = g.getTransform();
            g.rotate(Math.toRadians(i * 72));
            g.setColor(colors[i]);
            // ellipse rx=15 ry=26 centered at (0,-22)
            g.fill(new Ellipse2D.Double(-15, -22 - 26, 30, 52));
            g.setTransform(saved);
        }
        g.setColor(ROSE);
        g.fill(new Ellipse2D.Double(-13, -13, 26, 26));
        g.setColor(BLUSH);
        g.fill(new Ellipse2D.Double(-6, -6, 12, 12));
        g.setStroke(new BasicStroke(1));
        g.dispose();
        return image;
    }

    private static void write(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }

    private static final String FAVICON = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
            + "<g transform=\"translate(50 50)\">"
            + "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"#F472B6\"/>"
            + "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"#F9A8D4\" transform=\"rotate(72)\"/>"
            + "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"#F472B6\" transform=\"rotate(144)\"/>"
            + "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"#F9A8D4\" transform=\"rotate(216)\"/>"
            + "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"#F472B6\" transform=\"rotate(288)\"/>"
            + "<circle r=\"13\" fill=\"#BE185D\"/><circle r=\"6\" fill=\"#FFF0F5\"/></g></svg>\n";
}
