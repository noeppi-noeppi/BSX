package bsx.compiler.preprocessor;

import bsx.regex.VimRegex;

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
        String replacement = m.group(2).replace("\\/", "/");

        boolean global = false;
        boolean ignoreCase = false;

        for (char chr : m.group(3).toCharArray()) {
            switch (chr) {
                case 'g' -> global = true;
                case 'i' -> ignoreCase = true;
                case 'I' -> ignoreCase = false;
                case 'c' -> {} // confirm in vim, ignored here
                default -> throw new IllegalArgumentException("Invalid regex modifier: " + chr);
            }
        }

        return new Macro(VimRegex.compile(pattern, global, ignoreCase), replacement);
    }

    private record Macro(VimRegex.Pattern regex, String replacement) {

        public String applyTo(String line) {
            return VimRegex.replace(line, regex, replacement);
        }
    }
}
