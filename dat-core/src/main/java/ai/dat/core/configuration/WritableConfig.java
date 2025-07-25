package ai.dat.core.configuration;


/**
 * Write access to a configuration object.
 * Allows storing values described with meta information included in {@link ConfigOption}.
 */
public interface WritableConfig {

    /**
     * Stores a given value using the metadata included in the {@link ConfigOption}.
     * The value should be readable back through {@link ReadableConfig}.
     *
     * @param option metadata information
     * @param value  value to be stored
     * @param <T>    type of the value to be stored
     * @return instance of this configuration for fluent API
     */
    <T> WritableConfig set(ConfigOption<T> option, T value);
}
