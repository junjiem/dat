package ai.dat.core.project.data;

import lombok.NonNull;

import java.util.List;

public record FileChanges(@NonNull List<SchemaFileState> newFiles,
                          @NonNull List<SchemaFileState> modifiedFiles,
                          @NonNull List<SchemaFileState> unchangedFiles,
                          @NonNull List<SchemaFileState> deletedFiles) {
    /**
     * 检查是否有文件变化
     *
     * @return 是否存在文件变化（新增、修改或删除）
     */
    public boolean hasChanges() {
        return !newFiles.isEmpty() || !modifiedFiles.isEmpty() || !deletedFiles.isEmpty();
    }

    /**
     * 获取总变化文件数量
     *
     * @return 变化文件总数
     */
    public int getTotalChanges() {
        return newFiles.size() + modifiedFiles.size() + deletedFiles.size();
    }
}