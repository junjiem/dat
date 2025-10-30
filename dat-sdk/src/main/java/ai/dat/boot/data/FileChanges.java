package ai.dat.boot.data;

import lombok.NonNull;

import java.util.List;

/**
 * Aggregates schema file changes detected during an incremental build.
 */
public record FileChanges(@NonNull List<SchemaFileState> newFiles,
                          @NonNull List<SchemaFileState> modifiedFiles,
                          @NonNull List<SchemaFileState> unchangedFiles,
                          @NonNull List<SchemaFileState> deletedFiles) {
    /**
     * Indicates whether any files were added, modified, or deleted.
     *
     * @return {@code true} if changes exist, otherwise {@code false}
     */
    public boolean hasChanges() {
        return !newFiles.isEmpty() || !modifiedFiles.isEmpty() || !deletedFiles.isEmpty();
    }

    /**
     * Calculates the total number of changed files.
     *
     * @return the total number of new, modified, and deleted files
     */
    public int getTotalChanges() {
        return newFiles.size() + modifiedFiles.size() + deletedFiles.size();
    }
}