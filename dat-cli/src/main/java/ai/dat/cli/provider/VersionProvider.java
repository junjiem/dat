package ai.dat.cli.provider;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.IVersionProvider;

import java.io.InputStream;
import java.util.Properties;

/**
 * @Author JunjieM
 * @Date 2025/8/26
 */
@Slf4j
public class VersionProvider implements IVersionProvider {

    private static final String DEFAULT_VERSION = "0.0.1";
    private static final String VERSION_KEY = "version";

    private static String cachedVersion;

    @Override
    public String[] getVersion() throws Exception {
        return new String[]{"DAT CLI v" + version()};
    }

    /**
     * 获取应用版本号
     * 优先从Maven属性文件读取，如果读取失败则使用默认版本
     */
    public static String version() {
        if (cachedVersion == null) {
            cachedVersion = loadVersionFromManifest();
        }
        return cachedVersion;
    }

    /**
     * 从Maven生成的属性文件加载版本信息
     */
    private static String loadVersionFromManifest() {
        try {
            // 尝试从META-INF/maven目录读取pom.properties
            InputStream is = VersionProvider.class.getClassLoader()
                    .getResourceAsStream("META-INF/maven/ai.dat/dat-cli/pom.properties");
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                String version = properties.getProperty(VERSION_KEY);
                if (version != null && !version.trim().isEmpty()) {
                    log.debug("Loaded version from pom.properties: {}", version);
                    return version;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to load version from pom.properties: {}", e.getMessage());
        }

        // 尝试从包信息获取版本
        try {
            Package pkg = VersionProvider.class.getPackage();
            if (pkg != null) {
                String version = pkg.getImplementationVersion();
                if (version != null && !version.trim().isEmpty()) {
                    log.debug("Loaded version from package: {}", version);
                    return version;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to load version from package: {}", e.getMessage());
        }

        log.debug("Using default version: {}", DEFAULT_VERSION);
        return DEFAULT_VERSION;
    }
}
