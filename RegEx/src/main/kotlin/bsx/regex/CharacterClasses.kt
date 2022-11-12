/*
 * Taken from IdeaVim
 * https://github.com/JetBrains/ideavim/blob/master/vim-engine/src/main/kotlin/com/maddyhome/idea/vim/regexp/CharacterClasses.kt
 * Original license: See bottom.
 */
package bsx.regex

import org.jetbrains.annotations.NonNls

object CharacterClasses {
    val CLASS_NAMES: /*@NonNls*/ Array<String> = arrayOf(
        "alnum:]",
        "alpha:]",
        "blank:]",
        "cntrl:]",
        "digit:]",
        "graph:]",
        "lower:]",
        "print:]",
        "punct:]",
        "space:]",
        "upper:]",
        "xdigit:]",
        "tab:]",
        "return:]",
        "backspace:]",
        "escape:]"
    )
    const val RI_DIGIT = 0x01
    const val RI_HEX = 0x02
    const val RI_OCTAL = 0x04
    const val RI_WORD = 0x08
    const val RI_HEAD = 0x10
    const val RI_ALPHA = 0x20
    const val RI_LOWER = 0x40
    const val RI_UPPER = 0x80
    const val RI_WHITE = 0x100
    const val CLASS_ALNUM = 0
    const val CLASS_ALPHA = 1
    const val CLASS_BLANK = 2
    const val CLASS_CNTRL = 3
    const val CLASS_DIGIT = 4
    const val CLASS_GRAPH = 5
    const val CLASS_LOWER = 6
    const val CLASS_PRINT = 7
    const val CLASS_PUNCT = 8
    const val CLASS_SPACE = 9
    const val CLASS_UPPER = 10
    const val CLASS_XDIGIT = 11
    const val CLASS_TAB = 12
    const val CLASS_RETURN = 13
    const val CLASS_BACKSPACE = 14
    const val CLASS_ESCAPE = 15
    const val CLASS_NONE = 99
    fun isMask(ch: Char, mask: Int, test: Int): Boolean {
        var res = false
        when (mask) {
            RI_DIGIT -> res = Character.isDigit(ch)
            RI_HEX -> res = isHex(ch)
            RI_OCTAL -> res = isOctal(ch)
            RI_WORD -> res = isWord(ch)
            RI_HEAD -> res = isHead(ch)
            RI_ALPHA -> res = isAlpha(ch)
            RI_LOWER -> res = isLower(ch)
            RI_UPPER -> res = isUpper(ch)
            RI_WHITE -> res = isWhite(ch)
        }
        return res == test > 0
    }

    fun isHex(ch: Char): Boolean {
        return ch.digitToIntOrNull(16) ?: -1 != -1
    }

    fun isOctal(ch: Char): Boolean {
        return ch.digitToIntOrNull(8) ?: -1 != -1
    }

    fun isWord(ch: Char): Boolean {
        return Character.isLetterOrDigit(ch) || ch == '_'
    }

    fun isHead(ch: Char): Boolean {
        return Character.isLetter(ch) || ch == '_'
    }

    @JvmStatic
    fun isAlpha(ch: Char): Boolean {
        return Character.isLetter(ch)
    }

    fun isLower(ch: Char): Boolean {
        return Character.isLowerCase(ch)
    }

    fun isUpper(ch: Char): Boolean {
        return Character.isUpperCase(ch)
    }

    fun isWhite(ch: Char): Boolean {
        return Character.isWhitespace(ch)
    }

    fun isGraph(ch: Char): Boolean {
        return ch >= '!' && ch <= '~'
    }

    fun isPrint(ch: Char): Boolean {
        return ch >= ' ' && ch <= '~' || ch.code > 0xff
    }

    fun isPunct(ch: Char): Boolean {
        return ch in '!'..'/' ||
                ch in ':'..'@' ||
                ch in '['..'`' ||
                ch in '{'..'~'
    }

    fun isFile(ch: Char): Boolean {
        return isWord(ch) || "/.-+,#$%~=".indexOf(ch) != -1
    }
}

/*

Originally taken from IdeaVim: https://github.com/JetBrains/ideavim
IdeaVim is licensed under the following license:

MIT License

Copyright (c) 2003-present The IdeaVim authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */