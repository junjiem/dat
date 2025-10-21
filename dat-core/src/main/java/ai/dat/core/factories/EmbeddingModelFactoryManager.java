package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/6/30
 */
public class EmbeddingModelFactoryManager {
    private static final FactoryManager<EmbeddingModelFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(EmbeddingModelFactory.class, "embedding model");
    }

    public static EmbeddingModelFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}