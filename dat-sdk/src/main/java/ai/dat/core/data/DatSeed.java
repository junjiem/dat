package ai.dat.core.data;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * Represents a single seed data file along with validation on the file name and contents.
 */
@Getter
public class DatSeed {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z][A-Za-z0-9_]*$");

    /**
     * The logical file name of the seed data without extension.
     */
    @NonNull
    private String name;

    /**
     * The raw CSV content associated with the seed data.
     */
    @NonNull
    private String content;

    /**
     * Creates a new seed instance after validating the name format.
     *
     * @param name the seed file name
     * @param content the CSV content of the seed
     */
    private DatSeed(@NonNull String name, @NonNull String content) {
        Preconditions.checkArgument(PATTERN.matcher(name).matches(),
                "Invalid seed CSV file name: '%s'. " +
                        "Only letters, numbers and underscores (_) are allowed.", name);
        this.name = name;
        this.content = content;
    }

    /**
     * Factory method to create a {@link DatSeed} instance.
     *
     * @param name the seed file name
     * @param content the CSV content of the seed
     * @return a new {@link DatSeed}
     */
    public static DatSeed from(@NonNull String name, @NonNull String content) {
        return new DatSeed(name, content);
    }
}
