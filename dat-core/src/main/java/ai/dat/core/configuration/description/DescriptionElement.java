package ai.dat.core.configuration.description;


/**
 * Part of a {@link Description} that can be converted into String representation.
 */
interface DescriptionElement {
    /**
     * Transforms itself into String representation using given format.
     *
     * @param formatter formatter to use.
     */
    void format(Formatter formatter);
}
