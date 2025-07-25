package ai.dat.core.factories;

import com.google.common.base.Preconditions;
import dev.langchain4j.spi.ServiceHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class AskdataAgentFactoryManager {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");
    private static final Map<String, AskdataAgentFactory> factories = new HashMap<>();

    static {
        for (AskdataAgentFactory factory : ServiceHelper.loadFactories(AskdataAgentFactory.class)) {
            String identifier = factory.factoryIdentifier();
            Preconditions.checkArgument(
                    PATTERN.matcher(identifier).matches(),
                    "Invalid askdata agent factory identifier: '%s'. " +
                            "Only letters, numbers, underscores (_), and hyphens (-) are allowed.",
                    identifier
            );
            Preconditions.checkArgument(!factories.containsKey(identifier),
                    "There is already a askdata agent factory identifier with the same name:"
                            + identifier);
            factories.put(identifier, factory);
        }
    }

    public static AskdataAgentFactory getFactory(String identifier) {
        AskdataAgentFactory factory = factories.get(identifier);
        Preconditions.checkNotNull(factory,
                "Unsupported askdata agent factory identifier: " + identifier +
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
