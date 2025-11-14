package ai.dat.core.configuration;


import ai.dat.core.configuration.description.Description;
import com.google.common.base.Preconditions;
import lombok.Getter;


/**
 * A {@code ConfigOption} describes a configuration parameter.
 * It encapsulates the configuration key, and an optional default value for the configuration parameter.
 *
 * <p>{@code ConfigOptions} are built via the {@link ConfigOptions} class. Once created, a config
 * option is immutable.
 *
 * @param <T> The type of value associated with the configuration option.
 */
public class ConfigOption<T> {

    static final Description EMPTY_DESCRIPTION = Description.builder().text("").build();

    // ------------------------------------------------------------------------

    /**
     * The current key for that config option.
     */
    private final String key;

    /**
     * The default value for this option.
     */
    private final T defaultValue;

    /**
     * The description for this option.
     */
    private final Description description;

    /**
     * Type of the value that this ConfigOption describes.
     *
     * <ul>
     *   <li>typeClass == atomic class (e.g. {@code Integer.class}) -> {@code ConfigOption<Integer>}
     *   <li>typeClass == {@code Map.class} -> {@code ConfigOption<Map<String, String>>}
     *   <li>typeClass == atomic class and isList == true for {@code ConfigOption<List<Integer>>}
     * </ul>
     */
    @Getter
    private final Class<?> clazz;

    @Getter
    private final boolean isList;

    // ------------------------------------------------------------------------

    /**
     * Creates a new config option with fallback keys.
     *
     * @param key          The current key for that config option
     * @param clazz        describes type of the ConfigOption, see description of the clazz field
     * @param description  Description for that option
     * @param defaultValue The default value for this option
     * @param isList       tells if the ConfigOption describes a list option, see description of the clazz
     *                     field
     */
    ConfigOption(
            String key,
            Class<?> clazz,
            Description description,
            T defaultValue,
            boolean isList) {
        this.key = Preconditions.checkNotNull(key);
        this.description = description;
        this.defaultValue = defaultValue;
        this.clazz = Preconditions.checkNotNull(clazz);
        this.isList = isList;
    }

    // ------------------------------------------------------------------------

    /**
     * Creates a new config option, using this option's key and default value, and adding the given
     * description. The given description is used when generation the configuration documentation.
     *
     * @param description The description for this option.
     * @return A new config option, with given description.
     */
    public ConfigOption<T> withDescription(final String description) {
        return withDescription(Description.builder().text(description).build());
    }

    /**
     * Creates a new config option, using this option's key and default value, and adding the given
     * description. The given description is used when generation the configuration documentation.
     *
     * @param description The description for this option.
     * @return A new config option, with given description.
     */
    public ConfigOption<T> withDescription(final Description description) {
        return new ConfigOption<>(key, clazz, description, defaultValue, isList);
    }

    // ------------------------------------------------------------------------

    /**
     * Gets the configuration key.
     *
     * @return The configuration key
     */
    public String key() {
        return key;
    }

    /**
     * Checks if this option has a default value.
     *
     * @return True if it has a default value, false if not.
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Returns the default value, or null, if there is no default value.
     *
     * @return The default value, or null.
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the description of this option.
     *
     * @return The option's description.
     */
    public Description description() {
        return description;
    }

    // ------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && o.getClass() == ConfigOption.class) {
            ConfigOption<?> that = (ConfigOption<?>) o;
            return this.key.equals(that.key)
                    && (this.defaultValue == null
                    ? that.defaultValue == null
                    : (that.defaultValue != null
                    && this.defaultValue.equals(that.defaultValue)));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode()
                + (defaultValue != null ? defaultValue.hashCode() : 0);
    }

    @Override
    public String toString() {
        return String.format(
                "Key: '%s' , default: %s",
                key, defaultValue);
    }
}
