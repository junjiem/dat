package ai.dat.core.factories;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.configuration.ReadableConfig;
import lombok.NonNull;

/**
 * 数据库适配器工厂接口类
 */
public interface DatabaseAdapterFactory extends Factory {
    DatabaseAdapter create(ReadableConfig config);
}
