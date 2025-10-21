package ai.dat.core.factories;

import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class AskdataAgentFactoryManager {
    private static final FactoryManager<AskdataAgentFactory> factoryManager;

    static {
        factoryManager = new FactoryManager<>(AskdataAgentFactory.class, "askdata agent");
    }

    public static AskdataAgentFactory getFactory(String identifier) {
        return factoryManager.getFactory(identifier);
    }

    public static Set<String> getSupports() {
        return factoryManager.getSupports();
    }

    public static boolean isSupported(String identifier) {
        return factoryManager.isSupported(identifier);
    }
}
