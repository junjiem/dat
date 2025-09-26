package ai.dat.boot;

import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.ProjectUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * 构建状态管理器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
class BuildStateManager {

    private static final String STATE_FILE_PREFIX = "build_state_";
    private static final String STATE_FILE_SUFFIX = ".json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path datDir;
    private final ReentrantReadWriteLock lock;

    public BuildStateManager(@NonNull Path projectPath) {
        this.datDir = projectPath.resolve(ProjectUtil.DAT_DIR_NAME);
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * 加载指定配置的构建状态
     */
    public List<SchemaFileState> loadBuildState(@NonNull String stateId) throws IOException {
        return withReadLock(() -> {
            Path file = getStateFile(stateId);
            if (!Files.exists(file)) {
                return Collections.emptyList();
            }
            try {
                return OBJECT_MAPPER.readValue(file.toFile(), new TypeReference<>() {
                });
            } catch (Exception e) {
                throw new RuntimeException("The build state file " + file.getFileName()
                        + " loading failed", e);
            }
        });
    }

    /**
     * 保存指定配置的构建状态
     */
    public void saveBuildState(@NonNull String stateId,
                               @NonNull List<SchemaFileState> fileStates) throws IOException {
        withWriteLock(() -> {
            ensureDatDirectory();
            Path file = getStateFile(stateId);
            OBJECT_MAPPER.writeValue(file.toFile(), fileStates);
            return null;
        });
    }

    /**
     * 清理指定配置的状态
     */
    public void cleanState(@NonNull String stateId) throws IOException {
        withWriteLock(() -> {
            Path file = getStateFile(stateId);
            if (Files.exists(file)) {
                Files.delete(file);
                log.info("Clean up the state file: {}", file.getFileName());
            }
            return null;
        });
    }

    /**
     * 清理所有状态文件
     */
    public void cleanAllState() throws IOException {
        withWriteLock(() -> {
            if (!Files.exists(datDir)) {
                return null;
            }
            List<Path> files = listStateFiles();
            for (Path file : files) {
                Files.delete(file);
                log.info("Clean the expired state and embedding files: {}", file.getFileName());
            }
            log.info("Cleared {} expired state and embedding files", files.size());
            return null;
        });
    }

    /**
     * 清理过期的状态文件（保留最近N个配置的状态）
     */
    public void cleanOldStates(int keepCount) throws IOException {
        Preconditions.checkArgument(keepCount > 0, "keepCount must be greater than 0");
        withWriteLock(() -> {
            if (!Files.exists(datDir)) {
                return null;
            }
            List<Path> stateFiles = listStateFilesByModifiedTime();
            if (stateFiles.size() > keepCount) {
                List<Path> files = stateFiles.subList(keepCount, stateFiles.size());
                for (Path file : files) {
                    Files.delete(file);
                    log.info("Clean the expired state files: {}", file.getFileName());
                }
                log.info("Cleared {} expired state files and retained the latest {}", files.size(), keepCount);
            }
            List<Path> embeddingFiles = listEmbeddingFilesByModifiedTime();
            if (embeddingFiles.size() > keepCount * 2) {
                List<Path> files = embeddingFiles.subList(keepCount * 2, embeddingFiles.size());
                for (Path file : files) {
                    Files.delete(file);
                    log.info("Clean the expired embedding files: {}", file.getFileName());
                }
                log.info("Cleared {} expired embedding files and retained the latest {}",
                        files.size(), keepCount * 2);
            }
            return null;
        });
    }

    private Path getStateFile(String stateId) {
        return datDir.resolve(STATE_FILE_PREFIX + stateId + STATE_FILE_SUFFIX);
    }

    private void ensureDatDirectory() throws IOException {
        if (!Files.exists(datDir)) {
            Files.createDirectories(datDir);
        }
    }

    private List<Path> listStateFiles() throws IOException {
        try (Stream<Path> files = Files.list(datDir)) {
            return files.filter(path -> {
                        String fileName = path.getFileName().toString();
                        return (fileName.startsWith(STATE_FILE_PREFIX) && fileName.endsWith(STATE_FILE_SUFFIX))
                                || fileName.startsWith(ProjectUtil.DUCKDB_EMBEDDING_STORE_FILE_PREFIX);
                    })
                    .toList();
        }
    }

    private List<Path> listStateFilesByModifiedTime() throws IOException {
        try (Stream<Path> files = Files.list(datDir)) {
            return files.filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith(STATE_FILE_PREFIX) && fileName.endsWith(STATE_FILE_SUFFIX);
                    })
                    .sorted((o1, o2) -> {
                        try {
                            return Files.getLastModifiedTime(o2).compareTo(Files.getLastModifiedTime(o1));
                        } catch (IOException e) {
                            log.warn("The comparison of file modification times failed: {} vs {}", o1, o2, e);
                            return 0;
                        }
                    })
                    .toList();
        }
    }

    private List<Path> listEmbeddingFilesByModifiedTime() throws IOException {
        try (Stream<Path> files = Files.list(datDir)) {
            return files.filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith(ProjectUtil.DUCKDB_EMBEDDING_STORE_FILE_PREFIX);
                    })
                    .sorted((o1, o2) -> {
                        try {
                            return Files.getLastModifiedTime(o2).compareTo(Files.getLastModifiedTime(o1));
                        } catch (IOException e) {
                            log.warn("The comparison of file modification times failed: {} vs {}", o1, o2, e);
                            return 0;
                        }
                    })
                    .toList();
        }
    }

    // 锁操作辅助方法
    private <T> T withReadLock(IOSupplier<T> supplier) throws IOException {
        lock.readLock().lock();
        try {
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private <T> T withWriteLock(IOSupplier<T> supplier) throws IOException {
        lock.writeLock().lock();
        try {
            return supplier.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }
}