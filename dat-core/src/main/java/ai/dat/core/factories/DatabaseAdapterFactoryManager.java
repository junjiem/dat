package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class DatabaseAdapterFactoryManager {
    private static final FactoryManager<DatabaseAdapterFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(DatabaseAdapterFactory.class, "database adapter");
    }

    public static DatabaseAdapterFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}
