package ai.dat.core.configuration.description;


import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a list in the {@link Description}.
 */
@Getter
public class ListElement implements BlockElement {

    private final List<InlineElement> entries;

    /**
     * Creates a list with blocks of text. For example:
     *
     * <pre>{@code
     * .list(
     * 	text("this is first element of list"),
     * 	text("this is second element of list with a %s", link("https://link"))
     * )
     * }</pre>
     *
     * @param elements list of this list entries
     * @return list representation
     */
    public static ListElement list(InlineElement... elements) {
        return new ListElement(Arrays.asList(elements));
    }

    private ListElement(List<InlineElement> entries) {
        this.entries = entries;
    }

    @Override
    public void format(Formatter formatter) {
        formatter.format(this);
    }
}
