package ai.dat.core.factories;

import com.google.common.base.Preconditions;
import dev.langchain4j.spi.ServiceHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class DatabaseAdapterFactoryManager {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");
    private static final Map<String, DatabaseAdapterFactory> factories = new HashMap<>();

    static {
        for (DatabaseAdapterFactory factory : ServiceHelper.loadFactories(DatabaseAdapterFactory.class)) {
            String identifier = factory.factoryIdentifier();
            Preconditions.checkArgument(
                    PATTERN.matcher(identifier).matches(),
                    "Invalid database adapter factory identifier: '%s'. " +
                            "Only letters, numbers, underscores (_), and hyphens (-) are allowed.",
                    identifier
            );
            Preconditions.checkArgument(!factories.containsKey(identifier),
                    "There is already a database adapter factory identifier with the same name:"
                            + identifier);
            factories.put(identifier, factory);
        }
    }

    public static DatabaseAdapterFactory getFactory(String identifier) {
        DatabaseAdapterFactory factory = factories.get(identifier);
        Preconditions.checkNotNull(factory,
                "Unsupported database adapter factory identifier: " + identifier +
                        ". Supported: " + String.join(", ", factories.keySet())
        );
        return factory;
    }

    public static Set<String> getSupports() {
        return factories.keySet();
    }

    public static boolean isSupported(String identifier) {
        return factories.containsKey(identifier);
    }
}
