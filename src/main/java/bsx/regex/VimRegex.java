package bsx.regex;

import javax.annotation.Nullable;

public class VimRegex {
    
    public static Pattern compile(String pattern) {
        return compile(pattern, false, false);
    }
    
    public static Pattern compile(String pattern, boolean global, boolean ignoreCase) {
        return new Pattern(pattern, new RegExp().vim_regcomp(pattern, 1), global, ignoreCase);
    }
    
    public static String replace(String text, Pattern pattern, String replacement) {
        VimEditor ve = new VimEditor(text);
        RegExp engine = new RegExp();
        RegExp.regmmatch_T rmp = new RegExp.regmmatch_T();
        rmp.regprog = pattern.prog;
        rmp.rmm_ic = pattern.ignoreCase;
        if (engine.vim_regexec_multi(rmp, ve, 1, 0, 0) == 0) {
            return text;
        }
        
        RegExp.lpos_T start = rmp.startpos[0];
        RegExp.lpos_T end = rmp.endpos[0];
        if (invalidIndices(text, start, end)) {
            return text;
        } else {
            String prefix = text.substring(0, start.col);
            String tail = text.substring(end.col);
            
            StringBuilder builtReplacement = new StringBuilder();
            boolean backslash = false;
            for (char chr : replacement.toCharArray()) {
                if (backslash) {
                    if (chr >= '0' && chr <= '9') {
                        String rep = getMatchGroup(text, rmp, chr - '0');
                        if (rep != null) builtReplacement.append(rep);
                    } else {
                        builtReplacement.append(chr);
                    }
                } else {
                    if (chr == '\\') {
                        backslash = true;
                    } else {
                        builtReplacement.append(chr);
                    }
                }
            }
            
            // Replace a potential second match in tail
            return prefix + builtReplacement + (pattern.global ? replace(tail, pattern, replacement) : tail);
        }
    }
    
    @Nullable
    private static String getMatchGroup(String text, RegExp.regmmatch_T rmp, int idx) {
        if (idx < 0 || idx >= 10) return null;
        RegExp.lpos_T start = rmp.startpos[idx];
        RegExp.lpos_T end = rmp.endpos[idx];
        if (invalidIndices(text, start, end)) {
            return null;
        } else {
            return text.substring(start.col, end.col);
        }
    }
    
    private static boolean invalidIndices(String text, RegExp.lpos_T start, RegExp.lpos_T end) {
        return start.lnum != 0 || start.col < 0 || start.col > text.length() || end.lnum != 0 || end.col < 0 || end.col > text.length() || end.col < start.col;
    }
    
    public static class Pattern {
        
        private final String patternStr;
        private final RegExp.regprog_T prog;
        private final boolean global;
        private final boolean ignoreCase;

        private Pattern(String patternStr, RegExp.regprog_T prog, boolean global, boolean ignoreCase) {
            this.patternStr = patternStr;
            this.prog = prog;
            this.global = global;
            this.ignoreCase = ignoreCase;
        }

        @Override
        public String toString() {
            return this.patternStr;
        }
    }
}
