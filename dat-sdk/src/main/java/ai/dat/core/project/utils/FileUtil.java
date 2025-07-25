package ai.dat.core.project.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileUtil {

    private FileUtil() {
    }

    public static String md5(@NonNull Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            return DigestUtils.md5Hex(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("The read " + filePath + " file md5 hash failed", e);
        }
    }

    public static long lastModified(@NonNull Path filePath) {
        try {
            return Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            throw new RuntimeException("The read " + filePath + " file last modified time failed", e);
        }
    }

    public static boolean exists(@NonNull Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
}