package app.studybloom.ui.components;

import app.studybloom.service.BloomProgress;
import app.studybloom.ui.Component;
import app.studybloom.ui.Dom;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * The signature element: a five-petal flower, one petal per 20% of this
 * week's tasks completed.
 */
public class BloomMeter extends Component {
    private BloomProgress progress;
    private HTMLElement flower;
    private HTMLElement headline;
    private HTMLElement message;

    public BloomMeter(BloomProgress progress) {
        this.progress = progress;
    }

    @Override
    protected HTMLElement build() {
        HTMLElement card = Dom.div("bloom-card");
        card.setAttribute("role", "status");
        flower = Dom.svg("div", "bloom-flower", flowerSvg(progress, -1));
        HTMLElement text = Dom.div("bloom-text");
        headline = Dom.text("div", "bt", progress.getHeadline());
        message = Dom.text("div", "bs", progress.getMessage());
        Dom.append(text, headline, message);
        Dom.append(card, flower, text);
        return card;
    }

    /** Redraws with new numbers; a newly filled petal gets a small grow animation. */
    public void update(BloomProgress next) {
        int grown = next.getFilledPetals() > progress.getFilledPetals() ? next.getFilledPetals() - 1 : -1;
        progress = next;
        if (flower == null) {
            return;
        }
        flower.setInnerHTML(flowerSvg(next, grown));
        headline.setTextContent(next.getHeadline());
        message.setTextContent(next.getMessage());
    }

    static String flowerSvg(BloomProgress progress, int growIndex) {
        StringBuilder svg = new StringBuilder("<svg width=\"64\" height=\"64\" viewBox=\"0 0 100 100\" role=\"img\" aria-label=\"")
                .append(progress.getFilledPetals()).append(" of 5 petals filled. ")
                .append(progress.getHeadline()).append("\"><g transform=\"translate(50 50)\">");
        for (int i = 0; i < BloomProgress.PETALS; i++) {
            String cls = "petal" + (i < progress.getFilledPetals() ? " on" : "") + (i == growIndex ? " grow" : "");
            // Rotation sits on the group so the CSS grow animation can scale the petal itself.
            svg.append("<g transform=\"rotate(").append(i * 72).append(")\"><ellipse class=\"").append(cls)
                    .append("\" rx=\"14\" ry=\"24\" cy=\"-22\"/></g>");
        }
        String label = progress.getTotal() == 0 ? "0" : progress.getCenterLabel();
        int fontSize = label.length() >= 5 ? 10 : label.length() == 4 ? 12 : 15;
        svg.append("<circle r=\"15\" fill=\"#BE185D\"/><text y=\"5\" text-anchor=\"middle\" font-family=\"Fredoka, sans-serif\"")
                .append(" font-weight=\"600\" font-size=\"").append(fontSize).append("\" fill=\"#fff\">")
                .append(label).append("</text></g></svg>");
        return svg.toString();
    }
}
