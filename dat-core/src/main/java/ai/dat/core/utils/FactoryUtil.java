package ai.dat.core.utils;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.*;
import ai.dat.core.semantic.data.SemanticModel;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Utility for working with {@link Factory}s.
 */
public final class FactoryUtil {

    public static final String DEFAULT_IDENTIFIER = "default";
    public static final String HIDDEN_CONTENT = "******";
    public static final String PLACEHOLDER_SYMBOL = "#";

    // the keys whose values should be hidden
    private static final String[] SENSITIVE_KEYS =
            new String[]{
                    "password",
                    "secret",
                    "account.key",
                    "apikey",
                    "api-key",
                    "auth-params",
                    "service-key",
                    "token",
                    "basic-auth",
                    "jaas.config"
            };

    public static final ConfigOption<String> PROVIDER =
            ConfigOptions.key("provider")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Uniquely identifies the provider that is used for accessing in an external system.");

    public static final ConfigOption<String> AGENT =
            ConfigOptions.key("agent")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Uniquely identifies the agent that is used for ask data.");

    private FactoryUtil() {
    }

    /**
     * Create Embedding Model
     *
     * @param identifier
     * @param config
     * @return
     */
    public static EmbeddingModel createEmbeddingModel(String identifier, ReadableConfig config) {
        EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(identifier);
        try {
            return factory.create(config);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create embedding model, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Create Embedding Store
     *
     * @param storeId
     * @param contentType
     * @param identifier
     * @param config
     * @return
     */
    public static EmbeddingStore<TextSegment> createEmbeddingStore(String storeId, ContentType contentType,
                                                                   String identifier, ReadableConfig config) {
        EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(identifier);
        try {
            return factory.create(storeId, contentType, config);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create %s embedding store, factory identifier: '%s'.",
                            contentType.name(), identifier), e);
        }
    }

    /**
     * Create Chat Model
     *
     * @param identifier
     * @param config
     * @return
     */
    public static ChatModel createChatModel(String identifier, ReadableConfig config) {
        ChatModelFactory factory = ChatModelFactoryManager.getFactory(identifier);
        try {
            return factory.create(config);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create LLM, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Create Streaming Chat Model
     *
     * @param identifier
     * @param config
     * @return
     */
    public static StreamingChatModel createStreamingChatModel(String identifier, ReadableConfig config) {
        ChatModelFactory factory = ChatModelFactoryManager.getFactory(identifier);
        try {
            return factory.createStream(config);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create streaming LLM, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Create Database Adapter
     *
     * @param identifier
     * @param config
     * @return
     */
    public static DatabaseAdapter createDatabaseAdapter(String identifier, ReadableConfig config) {
        DatabaseAdapterFactory factory = DatabaseAdapterFactoryManager.getFactory(identifier);
        try {
            return factory.create(config);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create database adapter, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Create Content Store
     *
     * @param storeId
     * @param identifier
     * @param embeddingModelIdentifier
     * @param embeddingModelConfig
     * @param embeddingStoreIdentifier
     * @param embeddingStoreConfig
     * @param chatModelIdentifier
     * @param chatModelConfig
     * @param config
     * @return
     */
    public static ContentStore createContentStore(String storeId,
                                                  String identifier,
                                                  ReadableConfig config,
                                                  String embeddingModelIdentifier,
                                                  ReadableConfig embeddingModelConfig,
                                                  String embeddingStoreIdentifier,
                                                  ReadableConfig embeddingStoreConfig,
                                                  String chatModelIdentifier,
                                                  ReadableConfig chatModelConfig) {
        ContentStoreFactory factory = ContentStoreFactoryManager.getFactory(identifier);
        EmbeddingModel embeddingModel = createEmbeddingModel(embeddingModelIdentifier, embeddingModelConfig);
        EmbeddingStore<TextSegment> mdlEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.MDL, embeddingStoreIdentifier, embeddingStoreConfig);
        EmbeddingStore<TextSegment> sqlEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.SQL, embeddingStoreIdentifier, embeddingStoreConfig);
        EmbeddingStore<TextSegment> synEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.SYN, embeddingStoreIdentifier, embeddingStoreConfig);
        EmbeddingStore<TextSegment> docEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.DOC, embeddingStoreIdentifier, embeddingStoreConfig);
        ChatModel chatModel = createChatModel(chatModelIdentifier, chatModelConfig);
        try {
            return factory.create(config, embeddingModel,
                    mdlEmbeddingStore, sqlEmbeddingStore, synEmbeddingStore, docEmbeddingStore, chatModel);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create content store, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Create Askdata Agent
     *
     * @param identifier
     * @param config
     * @param semanticModels
     * @param chatModelIdentifier
     * @param chatModelConfig
     * @param chatModelIdentifier
     * @param chatModelConfig
     * @param databaseAdapterIdentifier
     * @param databaseAdapterConfig
     * @return
     */
    public static AskdataAgent createAskdataAgent(String identifier,
                                                  ReadableConfig config,
                                                  List<SemanticModel> semanticModels,
                                                  ContentStore contentStore,
                                                  String chatModelIdentifier,
                                                  ReadableConfig chatModelConfig,
                                                  String databaseAdapterIdentifier,
                                                  ReadableConfig databaseAdapterConfig) {
        AskdataAgentFactory factory = AskdataAgentFactoryManager.getFactory(identifier);
        ChatModel chatModel = createChatModel(chatModelIdentifier, chatModelConfig);
        StreamingChatModel streamingChatModel = createStreamingChatModel(chatModelIdentifier, chatModelConfig);
        DatabaseAdapter databaseAdapter = createDatabaseAdapter(databaseAdapterIdentifier, databaseAdapterConfig);
        try {
            return factory.create(config, semanticModels, contentStore, chatModel, streamingChatModel, databaseAdapter);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create askdata agent, factory identifier: '%s'.",
                            identifier), e);
        }
    }

    /**
     * Validates the required and optional {@link ConfigOption}s of a factory.
     *
     * <p>Note: It does not check for left-over options.
     */
    public static void validateFactoryOptions(Factory factory, ReadableConfig options) {
        validateFactoryOptions(factory.requiredOptions(), factory.optionalOptions(), options);
    }

    /**
     * Validates the required options and optional options.
     *
     * <p>Note: It does not check for left-over options.
     */
    public static void validateFactoryOptions(
            Set<ConfigOption<?>> requiredOptions,
            Set<ConfigOption<?>> optionalOptions,
            ReadableConfig options) {
        // currently DAT's options have no validation feature which is why we access them eagerly
        // to provoke a parsing error
        final List<String> missingRequiredOptions =
                requiredOptions.stream()
                        // Templated options will never appear with their template key, so we need
                        // to ignore them as required properties here
                        .filter(option -> !option.key().contains(PLACEHOLDER_SYMBOL))
                        .filter(option -> readOption(options, option) == null)
                        .map(ConfigOption::key)
                        .sorted()
                        .collect(Collectors.toList());
        if (!missingRequiredOptions.isEmpty()) {
            throw new ValidationException(
                    String.format(
                            "One or more required options are missing.\n\n"
                                    + "Missing required options are:\n\n"
                                    + "%s",
                            String.join("\n", missingRequiredOptions)));
        }
        optionalOptions.forEach(option -> readOption(options, option));
    }

    /**
     * Validates unconsumed option keys.
     */
    public static void validateUnconsumedKeys(
            String factoryIdentifier,
            Set<String> allOptionKeys,
            Set<String> consumedOptionKeys,
            Set<String> deprecatedOptionKeys) {
        final Set<String> remainingOptionKeys = new HashSet<>(allOptionKeys);
        remainingOptionKeys.removeAll(consumedOptionKeys);
        if (!remainingOptionKeys.isEmpty()) {
            throw new ValidationException(
                    String.format(
                            "Unsupported options found for '%s'.\n\n"
                                    + "Unsupported options:\n\n"
                                    + "%s\n\n"
                                    + "Supported options:\n\n"
                                    + "%s",
                            factoryIdentifier,
                            remainingOptionKeys.stream().sorted().collect(Collectors.joining("\n")),
                            consumedOptionKeys.stream()
                                    .map(k -> {
                                        if (deprecatedOptionKeys.contains(k)) {
                                            return String.format("%s (deprecated)", k);
                                        }
                                        return k;
                                    })
                                    .sorted()
                                    .collect(Collectors.joining("\n"))));
        }
    }

    /**
     * Validates unconsumed option keys.
     */
    public static void validateUnconsumedKeys(
            String factoryIdentifier, Set<String> allOptionKeys, Set<String> consumedOptionKeys) {
        validateUnconsumedKeys(
                factoryIdentifier, allOptionKeys, consumedOptionKeys, Collections.emptySet());
    }

    public static String stringifyOption(String key, String value) {
        if (isSensitive(key)) {
            value = HIDDEN_CONTENT;
        }
        return String.format("%s: %s", key, value);
    }

    private static <T> T readOption(ReadableConfig options, ConfigOption<T> option) {
        try {
            return options.get(option);
        } catch (Throwable t) {
            throw new ValidationException(
                    String.format("Invalid value for option '%s'.", option.key()), t);
        }
    }

    public static boolean isSensitive(String key) {
        Preconditions.checkNotNull(key, "key is null");
        final String keyInLower = key.toLowerCase();
        for (String hideKey : SENSITIVE_KEYS) {
            if (keyInLower.length() >= hideKey.length() && keyInLower.contains(hideKey)) {
                return true;
            }
        }
        return false;
    }
}
