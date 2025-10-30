package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;

import java.util.Set;

/**
 * 工厂接口类
 */
public interface Factory {
    String factoryIdentifier();

    Set<ConfigOption<?>> requiredOptions();

    Set<ConfigOption<?>> optionalOptions();
}
