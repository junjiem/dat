package ai.dat.core.configuration.description;

import com.google.common.base.Strings;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Represents a text block in the {@link Description}.
 */
public class TextElement implements BlockElement, InlineElement {
    @Getter
    private final String format;
    @Getter
    private final List<InlineElement> elements;
    private final EnumSet<TextStyle> textStyles = EnumSet.noneOf(TextStyle.class);

    /**
     * Creates a block of text with placeholders ("%s") that will be replaced with proper string
     * representation of given {@link InlineElement}. For example:
     *
     * <p>{@code text("This is a text with a link %s", link("https://somepage", "to here"))}
     *
     * @param format   text with placeholders for elements
     * @param elements elements to be put in the text
     * @return block of text
     */
    public static TextElement text(String format, InlineElement... elements) {
        return new TextElement(format, Arrays.asList(elements));
    }

    /**
     * Creates a simple block of text.
     *
     * @param text a simple block of text
     * @return block of text
     */
    public static TextElement text(String text) {
        return new TextElement(text, Collections.emptyList());
    }

    /**
     * Wraps a list of {@link InlineElement}s into a single {@link TextElement}.
     */
    public static InlineElement wrap(InlineElement... elements) {
        return text(Strings.repeat("%s", elements.length), elements);
    }

    /**
     * Creates a block of text formatted as code.
     *
     * @param text a block of text that will be formatted as code
     * @return block of text formatted as code
     */
    public static TextElement code(String text) {
        TextElement element = text(text);
        element.textStyles.add(TextStyle.CODE);
        return element;
    }

    public EnumSet<TextStyle> getStyles() {
        return textStyles;
    }

    private TextElement(String format, List<InlineElement> elements) {
        this.format = format;
        this.elements = elements;
    }

    @Override
    public void format(Formatter formatter) {
        formatter.format(this);
    }

    /**
     * Styles that can be applied to {@link TextElement} e.g. code, bold etc.
     */
    public enum TextStyle {
        CODE
    }
}
