package bsx.compiler.preprocessor;

public class BlockResolver {

    public static String resolveBlocks(String code) {
        StringBuilder replaced = new StringBuilder();
        int lastLevel = 0;
        for (String line : code.split("\n")) {
            if (line.isBlank()) {
                // Add empty line, so line numbers reported by the parser still match
                replaced.append("\n");
                continue;
            }
            int level = getIndentationLevel(line);
            if (level > lastLevel) {
                replaced.append(PreProcessor.START_BLOCK.repeat(level - lastLevel));
            }
            if (level < lastLevel) {
                replaced.append(PreProcessor.END_BLOCK.repeat(lastLevel - level));
            }
            lastLevel = level;
            replaced.append(line).append("\n");
        }
        if (0 < lastLevel) {
            replaced.append(PreProcessor.END_BLOCK.repeat(lastLevel));
        }
        return replaced.toString();
    }

    private static int getIndentationLevel(String line) {
        int tabsLevel = (int) (2 * line.chars().takeWhile(chr -> chr == '\t').count());
        int spacesAfterTabs = (int) line.chars().dropWhile(chr -> chr == '\t').takeWhile(chr -> chr == ' ').count();
        if (spacesAfterTabs == 0 || spacesAfterTabs == 2) {
            return tabsLevel + (spacesAfterTabs / 2);
        } else {
            throw new IllegalStateException("Bad indentation");
        }
    }
}
