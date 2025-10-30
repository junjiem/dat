package ai.dat.boot.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for reading file metadata and hashing content within DAT projects.
 */
@Slf4j
public class FileUtil {

    /**
     * Utility class; prevent instantiation.
     */
    private FileUtil() {
    }

    /**
     * Calculates the MD5 hash of the specified file.
     *
     * @param filePath the path to the file
     * @return the MD5 hash as a hexadecimal string
     */
    public static String md5(@NonNull Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            return DigestUtils.md5Hex(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("The read " + filePath + " file md5 hash failed", e);
        }
    }

    /**
     * Retrieves the last modified timestamp of the specified file.
     *
     * @param filePath the path to the file
     * @return the last modified time in milliseconds since the epoch
     */
    public static long lastModified(@NonNull Path filePath) {
        try {
            return Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("The read " + filePath + " file last modified time failed", e);
        }
    }

    /**
     * Returns the file name without its suffix.
     *
     * @param fileName the original file name
     * @return the file name without the suffix
     */
    public static String fileNameWithoutSuffix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    /**
     * Checks whether the path exists and points to a regular file.
     *
     * @param filePath the path to check
     * @return {@code true} if the path exists and is a file, otherwise {@code false}
     */
    public static boolean exists(@NonNull Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
}