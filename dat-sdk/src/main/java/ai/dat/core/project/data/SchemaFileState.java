package ai.dat.core.project.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件元数据，记录文件的构建状态信息
 *
 * @Author JunjieM
 * @Date 2025/7/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaFileState {
    /**
     * 文件相对路径
     */
    private String relativePath;

    /**
     * 文件最后修改时间（毫秒时间戳）
     */
    private long lastModified;

    /**
     * 文件的MD5哈希值
     */
    private String md5Hash;

    /**
     * 向量存储ID列表
     */
    private List<String> vectorIds;

    /**
     * 依赖的模型文件信息
     */
    private List<ModelFileState> dependencies;
}