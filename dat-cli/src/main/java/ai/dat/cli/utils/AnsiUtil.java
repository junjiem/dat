package ai.dat.cli.utils;

import picocli.CommandLine.Help.Ansi;

/**
 * @Author JunjieM
 * @Date 2025/7/30
 */
public class AnsiUtil {
    private AnsiUtil() {
    }

    public static String string(String stringWithMarkup) {
        return Ansi.ON.string(stringWithMarkup);
    }
}
