package ai.dat.core.factories;

import java.util.Set;

/**
 *
 */
public class ChatModelFactoryManager {
    private static final FactoryManager<ChatModelFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(ChatModelFactory.class, "LLM (Chat Model)");
    }

    public static ChatModelFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}
