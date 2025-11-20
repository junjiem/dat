package ai.dat.storer.milvus;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.factories.EmbeddingStoreFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class MilvusEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String IDENTIFIER = "milvus";

    public static final String DEFAULT_COLLECTION_NAME_PREFIX = "dat_embeddings";

    public static final ConfigOption<String> HOST =
            ConfigOptions.key("host")
                    .stringType()
                    .defaultValue("localhost")
                    .withDescription("Host for Milvus instance");

    public static final ConfigOption<Integer> PORT =
            ConfigOptions.key("port")
                    .intType()
                    .defaultValue(19530)
                    .withDescription("Port for Milvus instance");

    public static final ConfigOption<String> USERNAME =
            ConfigOptions.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Username for Milvus");

    public static final ConfigOption<String> PASSWORD =
            ConfigOptions.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Password for Milvus");

    public static final ConfigOption<String> DATABASE_NAME =
            ConfigOptions.key("database-name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Name of the database");

    public static final ConfigOption<String> COLLECTION_NAME_PREFIX =
            ConfigOptions.key("collection-name-prefix")
                    .stringType()
                    .defaultValue(DEFAULT_COLLECTION_NAME_PREFIX)
                    .withDescription("Name prefix of the collection");

    public static final ConfigOption<Integer> DIMENSION =
            ConfigOptions.key("dimension")
                    .intType()
                    .noDefaultValue()
                    .withDescription("The dimensionality of the embedding vectors. " +
                            "This should match the embedding model being used.");

    public static final ConfigOption<ConsistencyLevelEnum> CONSISTENCY_LEVEL =
            ConfigOptions.key("consistency-level")
                    .enumType(ConsistencyLevelEnum.class)
                    .defaultValue(ConsistencyLevelEnum.EVENTUALLY)
                    .withDescription("Consistency level: STRONG, SESSION, BOUNDED or EVENTUALLY (default).");

    public static final ConfigOption<Boolean> AUTO_FLUSH_ON_INSERT =
            ConfigOptions.key("auto-flush-on-insert")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Auto flush after insert");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(HOST, PORT, USERNAME, PASSWORD, DIMENSION));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(DATABASE_NAME, COLLECTION_NAME_PREFIX,
                CONSISTENCY_LEVEL, AUTO_FLUSH_ON_INSERT));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(HOST, PORT, USERNAME, DATABASE_NAME, COLLECTION_NAME_PREFIX, DIMENSION);
    }

    @Override
    public EmbeddingStore<TextSegment> create(String storeId,
                                              ContentType contentType,
                                              ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String host = config.get(HOST);
        Integer port = config.get(PORT);
        String username = config.get(USERNAME);
        String password = config.get(PASSWORD);
        String collectionNamePrefix = config.get(COLLECTION_NAME_PREFIX);
        Integer dimension = config.get(DIMENSION);

        String collectionName = String.join("_",
                collectionNamePrefix,
                storeId.replace('-', '_'),
                contentType.getValue());

        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .collectionName(collectionName)
                .dimension(dimension);

        config.getOptional(DATABASE_NAME).ifPresent(builder::databaseName);
        config.getOptional(CONSISTENCY_LEVEL).ifPresent(builder::consistencyLevel);
        config.getOptional(AUTO_FLUSH_ON_INSERT).ifPresent(builder::autoFlushOnInsert);

        return builder.build();
    }
}
