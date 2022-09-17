package bsx.compiler.preprocessor;

public class PreProcessor {

    public static final String START_BLOCK = "\u0002";
    public static final String END_BLOCK = "\u0003";

    public static String preprocess(String code) {
        String lineFiltered = code.replace("\r", "");
        String withMacros = MacroApplication.applyMacros(lineFiltered);
        String withoutComments = CommentRemover.removeComments(withMacros);
        if (withoutComments.contains(START_BLOCK) || withoutComments.contains(END_BLOCK)) {
            throw new IllegalStateException("Invalid character in input");
        }
        return BlockResolver.resolveBlocks(withoutComments);
    }
}
