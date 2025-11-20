package ai.dat.storer.qdrant;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.factories.EmbeddingStoreFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class QdrantEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String IDENTIFIER = "qdrant";

    public static final String DEFAULT_COLLECTION_NAME_PREFIX = "dat_embeddings";

    public static final ConfigOption<String> HOST =
            ConfigOptions.key("host")
                    .stringType()
                    .defaultValue("localhost")
                    .withDescription("Qdrant host");

    public static final ConfigOption<Integer> PORT =
            ConfigOptions.key("port")
                    .intType()
                    .defaultValue(6333)
                    .withDescription("Qdrant port");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Qdrant API KEY");

    public static final ConfigOption<String> COLLECTION_NAME_PREFIX =
            ConfigOptions.key("collection-name-prefix")
                    .stringType()
                    .defaultValue(DEFAULT_COLLECTION_NAME_PREFIX)
                    .withDescription("Qdrant collection name prefix");

    public static final ConfigOption<io.qdrant.client.grpc.Collections.Distance> DISTANCE =
            ConfigOptions.key("distance")
                    .enumType(io.qdrant.client.grpc.Collections.Distance.class)
                    .defaultValue(io.qdrant.client.grpc.Collections.Distance.Cosine)
                    .withDescription("Qdrant collections distance. Supported: `Cosine`, `Euclid`, `Dot`, `Manhattan`.");

    public static final ConfigOption<Integer> DIMENSION =
            ConfigOptions.key("dimension")
                    .intType()
                    .noDefaultValue()
                    .withDescription("The dimensionality of the embedding vectors. " +
                            "This should match the embedding model being used.");

    public static final ConfigOption<Boolean> USE_TLS =
            ConfigOptions.key("use-tls")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Use Transport Layer Security");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(HOST, PORT, DIMENSION));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(API_KEY, COLLECTION_NAME_PREFIX, DISTANCE, USE_TLS));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(HOST, PORT, API_KEY, COLLECTION_NAME_PREFIX, DIMENSION);
    }

    @Override
    public EmbeddingStore<TextSegment> create(String storeId,
                                              ContentType contentType,
                                              ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String host = config.get(HOST);
        Integer port = config.get(PORT);
        String collectionNamePrefix = config.get(COLLECTION_NAME_PREFIX);
        Boolean useTls = config.get(USE_TLS);

        String collectionName = String.join("_",
                collectionNamePrefix,
                storeId.replace('-', '_'),
                contentType.getValue());

        QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .useTls(useTls);
        config.getOptional(API_KEY).ifPresent(builder::apiKey);

        try {
            QdrantGrpcClient.Builder clientBuilder = QdrantGrpcClient.newBuilder(host, port, useTls);
            config.getOptional(API_KEY).ifPresent(clientBuilder::withApiKey);
            QdrantClient client = new QdrantClient(clientBuilder.build());
            client.createCollectionAsync(collectionName,
                            io.qdrant.client.grpc.Collections.VectorParams.newBuilder()
                                    .setDistance(config.get(DISTANCE))
                                    .setSize(config.get(DIMENSION))
                                    .build())
                    .get();
            client.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return builder.build();
    }
}
