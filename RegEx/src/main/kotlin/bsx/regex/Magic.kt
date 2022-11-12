/*
 * Taken from IdeaVim
 * https://github.com/JetBrains/ideavim/blob/master/vim-engine/src/main/kotlin/com/maddyhome/idea/vim/regexp/Magic.kt
 * Original license: See bottom.
 */
package bsx.regex

object Magic {
    const val AMP = '&'.code - 256
    const val AT = '@'.code - 256
    const val DOLLAR = '$'.code - 256
    const val DOT = '.'.code - 256
    const val EQUAL = '='.code - 256
    const val GREATER = '>'.code - 256
    const val HAT = '^'.code - 256
    const val LBRACE = '['.code - 256
    const val LCURLY = '{'.code - 256
    const val LESS = '<'.code - 256
    const val LPAREN = '('.code - 256
    const val PERCENT = '%'.code - 256
    const val PIPE = '|'.code - 256
    const val PLUS = '+'.code - 256
    const val QUESTION = '?'.code - 256
    const val RPAREN = ')'.code - 256
    const val STAR = '*'.code - 256
    const val TILDE = '~'.code - 256
    const val UNDER = '_'.code - 256
    const val N0 = '0'.code - 256
    const val N1 = '1'.code - 256
    const val N2 = '2'.code - 256
    const val N3 = '3'.code - 256
    const val N4 = '4'.code - 256
    const val N5 = '5'.code - 256
    const val N6 = '6'.code - 256
    const val N7 = '7'.code - 256
    const val N8 = '8'.code - 256
    const val N9 = '9'.code - 256
    const val a = 'a'.code - 256
    const val A = 'A'.code - 256
    const val c = 'c'.code - 256
    const val C = 'C'.code - 256
    const val d = 'd'.code - 256
    const val D = 'D'.code - 256
    const val f = 'f'.code - 256
    const val F = 'F'.code - 256
    const val h = 'h'.code - 256
    const val H = 'H'.code - 256
    const val i = 'i'.code - 256
    const val I = 'I'.code - 256
    const val k = 'k'.code - 256
    const val K = 'K'.code - 256
    const val l = 'l'.code - 256
    const val L = 'L'.code - 256
    const val m = 'm'.code - 256
    const val M = 'M'.code - 256
    const val n = 'n'.code - 256
    const val N = 'N'.code - 256
    const val o = 'o'.code - 256
    const val O = 'O'.code - 256
    const val p = 'p'.code - 256
    const val P = 'P'.code - 256
    const val s = 's'.code - 256
    const val S = 'S'.code - 256
    const val u = 'u'.code - 256
    const val U = 'U'.code - 256
    const val v = 'v'.code - 256
    const val V = 'V'.code - 256
    const val w = 'w'.code - 256
    const val W = 'W'.code - 256
    const val x = 'x'.code - 256
    const val X = 'X'.code - 256
    const val z = 'z'.code - 256

    /*
   * Magic characters have a special meaning, they don't match literally.
   * Magic characters are negative.  This separates them from literal characters
   * (possibly multi-byte).  Only ASCII characters can be Magic.
   */
    fun magic(x: Int): Int {
        return x - 256
    }

    fun un_Magic(x: Int): Int {
        return x + 256
    }

    fun is_Magic(x: Int): Boolean {
        return x < 0
    }

    fun no_Magic(x: Int): Int {
        return if (is_Magic(x)) {
            un_Magic(x)
        } else x
    }

    fun toggle_Magic(x: Int): Int {
        return if (is_Magic(x)) {
            un_Magic(x)
        } else magic(x)
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
