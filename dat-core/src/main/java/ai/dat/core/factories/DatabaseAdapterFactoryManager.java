package ai.dat.core.factories;

import java.util.Set;

/**
 *
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
