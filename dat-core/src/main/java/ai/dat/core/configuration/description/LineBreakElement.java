package ai.dat.core.configuration.description;


/**
 * Represents a line break in the {@link Description}.
 */
public class LineBreakElement implements InlineElement, BlockElement {

    /**
     * Creates a line break in the description.
     */
    public static LineBreakElement linebreak() {
        return new LineBreakElement();
    }

    private LineBreakElement() {
    }

    @Override
    public void format(Formatter formatter) {
        formatter.format(this);
    }
}
