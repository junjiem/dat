package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/6/30
 */
public class EmbeddingStoreFactoryManager {
    private static final FactoryManager<EmbeddingStoreFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(EmbeddingStoreFactory.class, "embedding store");
    }

    public static EmbeddingStoreFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}