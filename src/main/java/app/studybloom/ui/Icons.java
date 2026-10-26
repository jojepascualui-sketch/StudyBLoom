package app.studybloom.ui;

/** SVG markup copied from the design reference. */
public final class Icons {
    private Icons() {
    }

    private static final String STROKE = "viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.2\""
            + " stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"";

    public static final String TASKS = "<svg " + STROKE + "><path d=\"M9 6h11M9 12h11M9 18h11\"/>"
            + "<circle cx=\"4.5\" cy=\"6\" r=\"1.4\"/><circle cx=\"4.5\" cy=\"12\" r=\"1.4\"/><circle cx=\"4.5\" cy=\"18\" r=\"1.4\"/></svg>";

    public static final String SCHEDULE = "<svg " + STROKE + "><rect x=\"3.5\" y=\"5\" width=\"17\" height=\"15\" rx=\"4\"/>"
            + "<path d=\"M3.5 10h17M8 3v4M16 3v4\"/></svg>";

    public static final String PLANS = "<svg " + STROKE + "><path d=\"M12 3l2.6 5.6 6 .7-4.5 4.1 1.2 6L12 16.4 6.7 19.4l1.2-6L3.4 9.3l6-.7z\"/></svg>";

    public static final String ME = "<svg " + STROKE + "><circle cx=\"12\" cy=\"8\" r=\"4\"/><path d=\"M4 20c1.5-4 4.5-6 8-6s6.5 2 8 6\"/></svg>";

    public static final String PLUS = "<svg width=\"28\" height=\"28\" viewBox=\"0 0 24 24\" aria-hidden=\"true\">"
            + "<path d=\"M12 5v14M5 12h14\" stroke=\"#fff\" stroke-width=\"2.8\" stroke-linecap=\"round\"/></svg>";

    public static final String CHECK = "<svg width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" aria-hidden=\"true\">"
            + "<path d=\"M5 12.5l4.5 4.5L19 7.5\" stroke=\"#fff\" stroke-width=\"3.2\" fill=\"none\""
            + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";

    /** The five-petal logo flower. */
    public static String logo(int size) {
        return "<svg width=\"" + size + "\" height=\"" + size + "\" viewBox=\"0 0 100 100\" aria-hidden=\"true\">"
                + "<g transform=\"translate(50 50)\">"
                + petal("#F472B6", 0) + petal("#F9A8D4", 72) + petal("#F472B6", 144)
                + petal("#F9A8D4", 216) + petal("#F472B6", 288)
                + "<circle r=\"13\" fill=\"#BE185D\"/><circle r=\"6\" fill=\"#FFF0F5\"/></g></svg>";
    }

    /** White flower used on the install card's app icon. */
    public static String appIcon() {
        StringBuilder svg = new StringBuilder("<svg width=\"34\" height=\"34\" viewBox=\"0 0 100 100\" aria-hidden=\"true\">"
                + "<g transform=\"translate(50 50)\">");
        for (int i = 0; i < 5; i++) {
            svg.append("<ellipse rx=\"14\" ry=\"24\" cy=\"-22\" fill=\"#fff\" transform=\"rotate(").append(i * 72).append(")\"/>");
        }
        return svg.append("<circle r=\"13\" fill=\"#BE185D\"/></g></svg>").toString();
    }

    private static String petal(String color, int rotation) {
        return "<ellipse rx=\"15\" ry=\"26\" cy=\"-22\" fill=\"" + color + "\" transform=\"rotate(" + rotation + ")\"/>";
    }
}
