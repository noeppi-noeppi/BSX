package bsx.compiler.preprocessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroApplication {

    private static final Pattern MACRO_PATTERN = Pattern.compile("/((?:\\\\/|[^/])+)/((?:\\\\/|[^/])+)/(\\w*)");

    public static String applyMacros(String code) {
        List<Macro> currentMacros = new ArrayList<>();
        StringBuilder replaced = new StringBuilder();
        for (String line : code.split("\n")) {
            Macro macro = parseMacro(line);
            if (macro != null) {
                currentMacros.add(macro);
                // Add empty line, so line numbers reported by the parser still match
                replaced.append("\n");
            } else {
                String replacedLine = line;
                for (Macro m : currentMacros) {
                    replacedLine = m.applyTo(replacedLine);
                }
                replaced.append(replacedLine).append("\n");
            }
        }
        return replaced.toString();
    }

    @Nullable
    private static Macro parseMacro(String line) {
        if (!line.strip().startsWith("#define")) return null;
        Matcher m = MACRO_PATTERN.matcher(line.strip().substring(7).strip());
        if (!m.matches()) return null;

        String pattern = m.group(1).replace("\\/", "/");

        String replacement = m.group(2)
                .replace("\\/", "/")
                .replace("$", "\\$")
                .replaceAll("\\\\(\\d+)", "\\$$1");

        StringBuilder flagsPart = new StringBuilder();
        boolean all = false;
        for (char chr : m.group(3).toCharArray()) {
            switch (chr) {
                case 'm' -> flagsPart.append("(?m)");
                case 's' -> flagsPart.append("(?s)");
                case 'i' -> flagsPart.append("(?i)");
                case 'x' -> flagsPart.append("(?x)");
                case 'u', 'a' -> flagsPart.append("(?u)");
                case 'g' -> all = true;
            }
        }

        return new Macro(Pattern.compile(flagsPart + pattern), replacement, all);
    }

    private record Macro(Pattern regex, String replacement, boolean all) {

        public String applyTo(String line) {
            if (this.all) {
                return this.regex.matcher(line).replaceAll(this.replacement);
            } else {
                return this.regex.matcher(line).replaceFirst(this.replacement);
            }
        }
    }
}
