package bsx.compiler.preprocessor;

public class CommentRemover {

    public static String removeComments(String code) {
        StringBuilder replaced = new StringBuilder();
        boolean inComment = false;
        boolean first = true;
        for (String line : code.split("\n")) {
            if (first) {
                first = false;
                // Allow for shebang
                if (line.startsWith("#!")) {
                    replaced.append("\n");
                    continue;
                }
            }
            switch (line) {
                case "\t " -> inComment = true;
                case " \t" -> inComment = false;
                default -> {
                    if (!inComment) {
                        int spacesAfterLeadingTabs = (int) line.chars().dropWhile(chr -> chr == '\t').takeWhile(chr -> chr == ' ').count();
                        if (spacesAfterLeadingTabs % 2 == 0) replaced.append(line);
                    }
                }
            }
            // Add a newline in any case, so line numbers reported by the parser still match
            replaced.append("\n");
        }
        return replaced.toString();
    }
}
