package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/10/21
 */
public class ScoringModelFactoryManager {
    private static final FactoryManager<ScoringModelFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(ScoringModelFactory.class, "scoring (re-ranking) model");
    }

    public static ScoringModelFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}