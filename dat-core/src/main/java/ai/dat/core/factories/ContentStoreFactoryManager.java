package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class ContentStoreFactoryManager {
    private static final FactoryManager<ContentStoreFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(ContentStoreFactory.class, "content store");
    }

    public static ContentStoreFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}
