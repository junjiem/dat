package ai.dat.cli.utils;

import picocli.CommandLine.Help.Ansi;

public class AnsiUtil {
    private AnsiUtil() {
    }

    public static String string(String stringWithMarkup) {
        return Ansi.ON.string(stringWithMarkup);
    }
}
