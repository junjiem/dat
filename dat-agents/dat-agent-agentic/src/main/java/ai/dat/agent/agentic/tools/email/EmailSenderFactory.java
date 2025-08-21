package ai.dat.agent.agentic.tools.email;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.utils.FactoryUtil;
import ai.dat.core.utils.YamlTemplateUtil;
import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/8/12
 */
public class EmailSenderFactory {

    private static final ConfigOption<String> SMTP_HOST =
            ConfigOptions.key("smtp-host")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("SMTP server address, such as: smtp.gmail.com");

    private static final ConfigOption<Integer> SMTP_PORT =
            ConfigOptions.key("smtp-port")
                    .intType()
                    .defaultValue(587)
                    .withDescription("SMTP server ports, commonly used ports: 25, 465, 587");

    private static final ConfigOption<Boolean> AUTH_ENABLED =
            ConfigOptions.key("auth-enabled")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether SMTP authentication is enabled");

    private static final ConfigOption<Boolean> TLS_ENABLED =
            ConfigOptions.key("tls-enabled")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether TLS encrypted transmission is enabled");

    private static final ConfigOption<String> USERNAME =
            ConfigOptions.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("SMTP authentication username (usually an email address)");

    private static final ConfigOption<String> PASSWORD =
            ConfigOptions.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("SMTP authentication password");

    private static final ConfigOption<String> FROM_ADDRESS =
            ConfigOptions.key("from-address")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Sender's email address");

    private static final ConfigOption<String> FROM_NAME =
            ConfigOptions.key("from-name")
                    .stringType()
                    .defaultValue("DAT Agent")
                    .withDescription("Sender display name");

    private static final ConfigOption<Duration> SMTP_CONNECTION_TIMEOUT =
            ConfigOptions.key("smtp-connection-timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(30))
                    .withDescription("SMTP connection timeout");

    private static final ConfigOption<Duration> SMTP_TIMEOUT =
            ConfigOptions.key("smtp-timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(30))
                    .withDescription("SMTP timeout");

    private static final ConfigOption<Duration> SMTP_WRITE_TIMEOUT =
            ConfigOptions.key("smtp-write-timeout")
                    .durationType()
                    .defaultValue(Duration.ofSeconds(30))
                    .withDescription("SMTP write timeout");

    private Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(SMTP_HOST, FROM_ADDRESS));
    }

    private Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(
                SMTP_PORT, AUTH_ENABLED, TLS_ENABLED, USERNAME, PASSWORD, FROM_NAME,
                SMTP_CONNECTION_TIMEOUT, SMTP_TIMEOUT, SMTP_WRITE_TIMEOUT
        ));
    }

    public String template() {
        String configuration = YamlTemplateUtil.getConfiguration(requiredOptions(), optionalOptions());
        return Arrays.stream(configuration.split("\n"))
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
    }

    public EmailSender create(ReadableConfig config) {
        validateConfigOptions(config);
        String smtpHost = config.get(SMTP_HOST);
        String fromAddress = config.get(FROM_ADDRESS);
        return EmailSender.builder()
                .smtpHost(smtpHost)
                .smtpPort(config.get(SMTP_PORT))
                .enableAuth(config.get(AUTH_ENABLED))
                .enableTls(config.get(TLS_ENABLED))
                .username(config.getOptional(USERNAME).orElse(null))
                .password(config.getOptional(PASSWORD).orElse(null))
                .fromAddress(fromAddress)
                .fromName(config.get(FROM_NAME))
                .smtpConnectionTimeout(config.get(SMTP_CONNECTION_TIMEOUT).toMillis())
                .smtpTimeout(config.get(SMTP_TIMEOUT).toMillis())
                .smtpWriteTimeout(config.get(SMTP_WRITE_TIMEOUT).toMillis())
                .build();
    }

    private void validateConfigOptions(ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(requiredOptions(), optionalOptions(), config);
        Integer smtpPort = config.get(SMTP_PORT);
        Preconditions.checkArgument(smtpPort > 0,
                "'" + SMTP_PORT.key() + "' value must be greater than 0");
    }

}
