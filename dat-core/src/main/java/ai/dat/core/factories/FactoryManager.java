package ai.dat.core.factories;

import com.google.common.base.Preconditions;
import dev.langchain4j.spi.ServiceHelper;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @Author JunjieM
 * @Date 2025/10/21
 */
public class FactoryManager<T extends Factory> {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    private final Map<String, T> factories = new HashMap<>();
    private final String description;

    public FactoryManager(@NonNull Class<T> clazz, @NonNull String description) {
        this.description = description;
        for (T factory : ServiceHelper.loadFactories(clazz)) {
            String identifier = factory.factoryIdentifier();
            Preconditions.checkArgument(
                    PATTERN.matcher(identifier).matches(),
                    "Invalid %s factory identifier: '%s'. " +
                            "Only letters, numbers, underscores (_), and hyphens (-) are allowed.",
                    description, identifier
            );
            Preconditions.checkArgument(!factories.containsKey(identifier),
                    "There is already a %s factory identifier with the same name:"
                    , description, identifier);
            factories.put(identifier, factory);
        }
    }

    public T getFactory(String identifier) {
        T factory = factories.get(identifier);
        Preconditions.checkNotNull(factory,
                "Unsupported %s factory identifier: %s. Supported: %s",
                description, identifier, String.join(", ", factories.keySet())
        );
        return factory;
    }

    public Set<String> getSupports() {
        return factories.keySet();
    }

    public boolean isSupported(String identifier) {
        return factories.containsKey(identifier);
    }
}