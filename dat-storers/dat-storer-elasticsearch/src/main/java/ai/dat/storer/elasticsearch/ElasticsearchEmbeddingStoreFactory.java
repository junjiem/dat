package ai.dat.storer.elasticsearch;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.factories.EmbeddingStoreFactory;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class ElasticsearchEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String IDENTIFIER = "elasticsearch";

    public static final String DEFAULT_INDEX_NAME_PREFIX = "dat_embeddings";

    public static final ConfigOption<String> SERVER_URL =
            ConfigOptions.key("server-url")
                    .stringType()
                    .defaultValue("https://localhost:9200")
                    .withDescription("ElasticSearch Server Url");

    public static final ConfigOption<String> API_KEY =
            ConfigOptions.key("api-key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("ElasticSearch API KEY");

    public static final ConfigOption<String> INDEX_NAME_PREFIX =
            ConfigOptions.key("index-name-prefix")
                    .stringType()
                    .defaultValue(DEFAULT_INDEX_NAME_PREFIX)
                    .withDescription("Name prefix of the ElasticSearch index");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new LinkedHashSet<>(List.of(SERVER_URL));
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(API_KEY, INDEX_NAME_PREFIX));
    }

    @Override
    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(SERVER_URL, INDEX_NAME_PREFIX);
    }

    @Override
    public EmbeddingStore<TextSegment> create(String storeId, ContentType contentType, ReadableConfig config) {
        FactoryUtil.validateFactoryOptions(this, config);

        String serverUrl = config.get(SERVER_URL);

        RestClient restClient;
        try {
            RestClientBuilder builder = RestClient.builder(HttpHost.create(serverUrl));
            config.getOptional(API_KEY).ifPresent(apiKey -> {
                builder.setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                });
            });
            restClient = builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch REST client for server URL: " + serverUrl, e);
        }

        String indexNamePrefix = config.get(INDEX_NAME_PREFIX);

        String indexName = String.join("_",
                indexNamePrefix,
                storeId.replace('-', '_'),
                contentType.getValue());

        return ElasticsearchEmbeddingStore.builder()
                .configuration(ElasticsearchConfigurationKnn.builder().build()) // default
                .restClient(restClient)
                .indexName(indexName)
                .build();
    }
}
