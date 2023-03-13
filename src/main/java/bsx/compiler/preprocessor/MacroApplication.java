package bsx.compiler.preprocessor;

import bsx.regex.VimRegex;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroApplication {

    private static final Pattern MACRO_PATTERN = Pattern.compile("/((?:\\\\/|[^/])+)/((?:\\\\/|[^/])+)/(\\w*)");

    public static String applyMacros(String code) {
        List<Macro> currentMacros = new ArrayList<>();
        StringBuilder replaced = new StringBuilder();
        int lnum = 0;
        for (String line : code.split("\n")) {
            lnum += 1;
            Macro macro = parseMacro(line);
            if (macro != null) {
                currentMacros.add(macro);
                // Add empty line, so line numbers reported by the parser still match
                replaced.append("\n");
            } else {
                String lastLine = line;
                for (int itr = 0; itr <= 100; itr++) {
                    if (itr == 100) throw new IllegalStateException("Too many levels of macro expansion on line " + lnum + ": " + line + " -> " + lastLine);
                    boolean changed = false;
                    for (Macro m : currentMacros) {
                        String newLine = m.applyTo(lastLine);
                        if (!Objects.equals(lastLine, newLine)) changed = true;
                        lastLine = newLine;
                    }
                    if (!changed) break;
                }
                replaced.append(lastLine).append("\n");
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
