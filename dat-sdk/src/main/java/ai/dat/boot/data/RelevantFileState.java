package ai.dat.boot.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author JunjieM
 * @Date 2025/7/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelevantFileState {
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
}