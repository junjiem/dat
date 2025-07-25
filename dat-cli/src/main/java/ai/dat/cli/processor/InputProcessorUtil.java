package ai.dat.cli.processor;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * @Author JunjieM
 * @Date 2025/7/23
 */
@Slf4j
public class InputProcessorUtil {

    private InputProcessorUtil() {
    }

    /**
     * 创建适合当前系统的输入处理器
     *
     * @return InputProcessor
     */
    public static InputProcessor createInputProcessor() {
        // 检测系统编码
        String systemEncoding = Charset.defaultCharset().displayName();
        String consoleEncoding = System.getProperty("console.encoding");
        String osName = System.getProperty("os.name").toLowerCase();

        log.info("System encoding: {}, Console encoding: {}, OS: {}",
                systemEncoding, consoleEncoding, osName);

        if (osName.contains("windows")) {
            // Windows 系统，尝试多种编码
            return new WindowsInputProcessor();
        } else {
            // Unix/Linux 系统，使用 UTF-8
            return new UnixInputProcessor();
        }
    }
}
