package ai.dat.core.data;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * @Author JunjieM
 * @Date 2025/9/15
 */
@Getter
public class DatSeed {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z][A-Za-z0-9_]*$");

    @NonNull
    private String name;

    @NonNull
    private String content;

    private DatSeed(@NonNull String name, @NonNull String content) {
        Preconditions.checkArgument(PATTERN.matcher(name).matches(),
                "Invalid seed CSV file name: '%s'. " +
                        "Only letters, numbers and underscores (_) are allowed.", name);
        this.name = name;
        this.content = content;
    }

    public static DatSeed from(@NonNull String name, @NonNull String content) {
        return new DatSeed(name, content);
    }
}
