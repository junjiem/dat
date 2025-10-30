package ai.dat.boot.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the persisted state of a project file that is relevant to build operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelevantFileState {
    /**
     * Relative path of the file within the project.
     */
    private String relativePath;

    /**
     * Last modification timestamp of the file, in milliseconds.
     */
    private long lastModified;

    /**
     * MD5 hash of the file contents.
     */
    private String md5Hash;
}