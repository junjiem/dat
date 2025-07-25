package ai.dat.adapter.postgresql;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.DatabaseAdapterFactory;
import org.postgresql.ds.PGSimpleDataSource;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class PostgreSqlDatabaseAdapterFactory implements DatabaseAdapterFactory {

    public static final String IDENTIFIER = "postgresql";

    public static final ConfigOption<String> URL =
            ConfigOptions.key("url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("PostgreSQL JDBC URL");

    public static final ConfigOption<String> USERNAME =
            ConfigOptions.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("PostgreSQL user name");

    public static final ConfigOption<String> PASSWORD =
            ConfigOptions.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("PostgreSQL password");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(60L))
                    .withDescription("PostgreSQL maximum timeout. " +
                            "The timeout should be in millisecond granularity.");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(URL, USERNAME, PASSWORD));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(TIMEOUT));
    }

    @Override
    public DatabaseAdapter create(ReadableConfig config) {
        String url = config.get(URL);
        Duration timeout = config.get(TIMEOUT);
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        config.getOptional(USERNAME).ifPresent(dataSource::setUser);
        config.getOptional(PASSWORD).ifPresent(dataSource::setPassword);
        dataSource.setConnectTimeout((int) timeout.toMillis());
        return new PostgreSqlDatabaseAdapter(dataSource);
    }
}
