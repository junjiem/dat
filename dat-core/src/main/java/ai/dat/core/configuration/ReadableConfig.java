package ai.dat.core.configuration;


import java.util.Map;
import java.util.Optional;

/**
 * Read access to a configuration object.
 * Allows reading values described with meta information included in {@link ConfigOption}.
 */
public interface ReadableConfig {

    /**
     * Reads a value using the metadata included in {@link ConfigOption}. Returns the {@link
     * ConfigOption#defaultValue()} if value key not present in the configuration.
     *
     * @param option metadata of the option to read
     * @param <T>    type of the value to read
     * @return read value or {@link ConfigOption#defaultValue()} if not found
     * @see #getOptional(ConfigOption)
     */
    <T> T get(ConfigOption<T> option);

    /**
     * Reads a value using the metadata included in {@link ConfigOption}. In contrast to {@link
     * #get(ConfigOption)} returns {@link Optional#empty()} if value not present.
     *
     * @param option metadata of the option to read
     * @param <T>    type of the value to read
     * @return read value or {@link Optional#empty()} if not found
     * @see #get(ConfigOption)
     */
    <T> Optional<T> getOptional(ConfigOption<T> option);

    /**
     * Converts the configuration items into a map of string key-value pairs.
     *
     * @return a map containing the configuration properties, where the keys are strings and the
     *     values are the corresponding configuration values in string format.
     */
    Map<String, String> toMap();
}
