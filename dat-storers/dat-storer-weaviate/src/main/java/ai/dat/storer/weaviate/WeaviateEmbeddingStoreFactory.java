package ai.dat.storer.weaviate;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.factories.EmbeddingStoreFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/6/30
 */
public class WeaviateEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String IDENTIFIER = "weaviate";

    public static final String DEFAULT_CLASS_NAME_PREFIX = "DatEmbeddings";

    public static final ConfigOption<String> SCHEME =
            ConfigOptions.key("scheme")
                    .stringType()
                    .defaultValue("http")
                    .withDescription("Weaviate scheme");

    public static final ConfigOption<String> HOST =
            ConfigOptions.key("host")
                    .stringType()
                    .defaultValue("localhost")
                    .withDescription("Weaviate host");

    public static final ConfigOption<Integer> PORT =
            ConfigOptions.key("port")
                    .intType()
                    .defaultValue(8080)
                    .withDescription("Weaviate port");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Weaviate API KEY");

    public static final ConfigOption<String> CLASS_NAME_PREFIX =
            ConfigOptions.key("class-name-prefix")
                    .stringType()
                    .defaultValue(DEFAULT_CLASS_NAME_PREFIX)
                    .withDescription("The object class prefix you want to store, e.g. \"MyGreatClass\". " +
                            "Must start from an uppercase letter.");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(SCHEME, HOST, PORT));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(API_KEY, CLASS_NAME_PREFIX));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(HOST, PORT, API_KEY, CLASS_NAME_PREFIX);
    }

    @Override
    public EmbeddingStore<TextSegment> create(String storeId,
                                              ContentType contentType,
                                              ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String scheme = config.get(SCHEME);
        String host = config.get(HOST);
        Integer port = config.get(PORT);
        String classNamePrefix = config.get(CLASS_NAME_PREFIX);

        String className = String.join("_",
                classNamePrefix,
                storeId.replace('-', '_'),
                contentType.getValue());

        WeaviateEmbeddingStore.WeaviateEmbeddingStoreBuilder builder = WeaviateEmbeddingStore.builder()
                .scheme(scheme)
                .host(host)
                .port(port)
                .objectClass(className);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);
        return builder.build();
    }
}