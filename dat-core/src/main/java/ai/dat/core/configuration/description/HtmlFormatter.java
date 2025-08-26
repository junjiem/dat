package ai.dat.core.configuration.description;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Formatter that transforms {@link Description} into Html representation.
 */
public class HtmlFormatter extends Formatter {

    @Override
    protected void formatLink(StringBuilder state, String link, String description) {
        state.append(String.format("<a href=\"%s\">%s</a>", link, description));
    }

    @Override
    protected void formatLineBreak(StringBuilder state) {
        state.append("<br />");
    }

    @Override
    protected void formatText(
            StringBuilder state,
            String format,
            String[] elements,
            EnumSet<TextElement.TextStyle> styles) {
        String escapedFormat = escapeCharacters(format);

        String prefix = "";
        String suffix = "";
        if (styles.contains(TextElement.TextStyle.CODE)) {
            prefix = "<code class=\"highlighter-rouge\">";
            suffix = "</code>";
        }
        state.append(prefix);
        state.append(String.format(escapedFormat, Arrays.stream(elements)));
        state.append(suffix);
    }

    @Override
    protected void formatList(StringBuilder state, String[] entries) {
        state.append("<ul>");
        for (String entry : entries) {
            state.append(String.format("<li>%s</li>", entry));
        }
        state.append("</ul>");
    }

    @Override
    protected Formatter newInstance() {
        return new HtmlFormatter();
    }

    private static String escapeCharacters(String value) {
        return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
}
