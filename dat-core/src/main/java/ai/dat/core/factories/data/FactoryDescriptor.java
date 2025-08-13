package ai.dat.core.factories.data;

import ai.dat.core.configuration.ReadableConfig;
import lombok.Getter;
import lombok.NonNull;

/**
 * @Author JunjieM
 * @Date 2025/8/8
 */
@Getter
public class FactoryDescriptor {
    @NonNull
    private String identifier;

    @NonNull
    private ReadableConfig config;

    private FactoryDescriptor(@NonNull String identifier,
                              @NonNull ReadableConfig config) {
        this.identifier = identifier;
        this.config = config;
    }

    public static FactoryDescriptor from(@NonNull String identifier,
                                         @NonNull ReadableConfig config) {
        return new FactoryDescriptor(identifier, config);
    }
}
