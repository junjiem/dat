package ai.dat.core.utils;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.ContentType;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.*;
import ai.dat.core.factories.data.ChatModelInstance;
import ai.dat.core.factories.data.FactoryDescriptor;
import ai.dat.core.semantic.data.SemanticModel;
import com.google.common.base.Preconditions;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Utility for working with {@link Factory}s.
 */
public final class FactoryUtil {

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

    private FactoryUtil() {
    }

    /**
     * Create Embedding Model
     *
     * @param factoryDescriptor
     * @return
     */
    public static EmbeddingModel createEmbeddingModel(@NonNull FactoryDescriptor factoryDescriptor) {
        EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        try {
            return factory.create(factoryDescriptor.getConfig());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create embedding model, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Embedding Store
     *
     * @param storeId
     * @param contentType
     * @param factoryDescriptor
     * @return
     */
    public static EmbeddingStore<TextSegment> createEmbeddingStore(@NonNull String storeId,
                                                                   @NonNull ContentType contentType,
                                                                   @NonNull FactoryDescriptor factoryDescriptor) {
        EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        try {
            return factory.create(storeId, contentType, factoryDescriptor.getConfig());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create %s embedding store, factory identifier: '%s'.",
                            contentType.name(), factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Chat Model
     *
     * @param factoryDescriptor
     * @return
     */
    public static ChatModel createChatModel(@NonNull FactoryDescriptor factoryDescriptor) {
        ChatModelFactory factory = ChatModelFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        try {
            return factory.create(factoryDescriptor.getConfig());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create LLM, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Streaming Chat Model
     *
     * @param factoryDescriptor
     * @return
     */
    public static StreamingChatModel createStreamingChatModel(@NonNull FactoryDescriptor factoryDescriptor) {
        ChatModelFactory factory = ChatModelFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        try {
            return factory.createStream(factoryDescriptor.getConfig());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create streaming LLM, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Database Adapter
     *
     * @param factoryDescriptor
     * @return
     */
    public static DatabaseAdapter createDatabaseAdapter(@NonNull FactoryDescriptor factoryDescriptor) {
        DatabaseAdapterFactory factory = DatabaseAdapterFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        try {
            return factory.create(factoryDescriptor.getConfig());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create database adapter, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Content Store
     *
     * @param storeId
     * @param factoryDescriptor
     * @param embeddingModelFactoryDescriptor
     * @param embeddingStoreFactoryDescriptor
     * @param chatModelFactoryDescriptors
     * @return
     */
    public static ContentStore createContentStore(@NonNull String storeId,
                                                  @NonNull FactoryDescriptor factoryDescriptor,
                                                  @NonNull FactoryDescriptor embeddingModelFactoryDescriptor,
                                                  @NonNull FactoryDescriptor embeddingStoreFactoryDescriptor,
                                                  @NonNull Map<String, FactoryDescriptor> chatModelFactoryDescriptors) {
        ContentStoreFactory factory = ContentStoreFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        EmbeddingModel embeddingModel = createEmbeddingModel(embeddingModelFactoryDescriptor);
        EmbeddingStore<TextSegment> mdlEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.MDL, embeddingStoreFactoryDescriptor);
        EmbeddingStore<TextSegment> sqlEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.SQL, embeddingStoreFactoryDescriptor);
        EmbeddingStore<TextSegment> synEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.SYN, embeddingStoreFactoryDescriptor);
        EmbeddingStore<TextSegment> docEmbeddingStore = createEmbeddingStore(
                storeId, ContentType.DOC, embeddingStoreFactoryDescriptor);
        List<ChatModelInstance> chatModelInstances = chatModelFactoryDescriptors.entrySet().stream()
                .map(e -> ChatModelInstance.from(e.getKey(), createChatModel(e.getValue()),
                        createStreamingChatModel(e.getValue())))
                .collect(Collectors.toList());
        try {
            return factory.create(factoryDescriptor.getConfig(), embeddingModel,
                    mdlEmbeddingStore, sqlEmbeddingStore, synEmbeddingStore, docEmbeddingStore, chatModelInstances);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create content store, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
        }
    }

    /**
     * Create Askdata Agent
     *
     * @param factoryDescriptor
     * @param semanticModels
     * @param contentStore
     * @param chatModelFactoryDescriptors
     * @param databaseAdapterFactoryDescriptor
     * @return
     */
    public static AskdataAgent createAskdataAgent(@NonNull FactoryDescriptor factoryDescriptor,
                                                  List<SemanticModel> semanticModels,
                                                  @NonNull ContentStore contentStore,
                                                  @NonNull Map<String, FactoryDescriptor> chatModelFactoryDescriptors,
                                                  @NonNull FactoryDescriptor databaseAdapterFactoryDescriptor) {
        AskdataAgentFactory factory = AskdataAgentFactoryManager.getFactory(factoryDescriptor.getIdentifier());
        DatabaseAdapter databaseAdapter = createDatabaseAdapter(databaseAdapterFactoryDescriptor);
        List<ChatModelInstance> chatModelInstances = chatModelFactoryDescriptors.entrySet().stream()
                .map(e -> ChatModelInstance.from(e.getKey(), createChatModel(e.getValue()),
                        createStreamingChatModel(e.getValue())))
                .collect(Collectors.toList());
        try {
            return factory.create(factoryDescriptor.getConfig(), semanticModels, contentStore,
                    chatModelInstances, databaseAdapter);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Failed to create askdata agent, factory identifier: '%s'.",
                            factoryDescriptor.getIdentifier()), e);
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
