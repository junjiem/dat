package ai.dat.adapter.mysql;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.factories.DatabaseAdapterFactory;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * @Author JunjieM
 * @Date 2025/7/4
 */
public class MySqlDatabaseAdapterFactory implements DatabaseAdapterFactory {

    public static final String IDENTIFIER = "mysql";

    public static final ConfigOption<String> URL =
            ConfigOptions.key("url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("MySQL JDBC URL");

    public static final ConfigOption<String> USERNAME =
            ConfigOptions.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("MySQL user name");

    public static final ConfigOption<String> PASSWORD =
            ConfigOptions.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("MySQL password");

    public static final ConfigOption<Duration> TIMEOUT =
            ConfigOptions.key("timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(60L))
                    .withDescription("MySQL maximum timeout. " +
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
        try {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setURL(url);
            config.getOptional(USERNAME).ifPresent(dataSource::setUser);
            config.getOptional(PASSWORD).ifPresent(dataSource::setPassword);
            dataSource.setConnectTimeout((int) timeout.toMillis());
            return new MySqlDatabaseAdapter(dataSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
