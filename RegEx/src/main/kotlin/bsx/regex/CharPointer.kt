/*
 * Taken from IdeaVim
 * https://github.com/JetBrains/ideavim/blob/master/vim-engine/src/main/kotlin/com/maddyhome/idea/vim/regexp/CharPointer.kt
 * Original license: See bottom.
 */
package bsx.regex

import java.lang.StringBuffer
import java.nio.CharBuffer
import java.util.*

class CharPointer {
    private var seq: CharSequence
    private var pointer = 0
    private var readonly: Boolean

    constructor(text: String) {
        seq = text
        readonly = true
    }

    constructor(text: CharBuffer) {
        seq = text
        readonly = true
    }

    constructor(text: StringBuffer) {
        seq = text
        readonly = false
    }

    private constructor(ptr: CharPointer, offset: Int) {
        seq = ptr.seq
        readonly = ptr.readonly
        pointer = ptr.pointer + offset
    }

    fun pointer(): Int {
        return pointer
    }

    @JvmOverloads
    fun set(ch: Char, offset: Int = 0): CharPointer {
        check(!readonly) { "readonly string" }
        val data = seq as StringBuffer
        while (pointer + offset >= data.length) {
            data.append('\u0000')
        }
        data.setCharAt(pointer + offset, ch)
        return this
    }

    fun charAtInc(): Char {
        val res = charAt(0)
        inc()
        return res
    }

    @JvmOverloads
    fun charAt(offset: Int = 0): Char {
        return if (end(offset)) {
            '\u0000'
        } else seq[pointer + offset]
    }

    @JvmOverloads
    operator fun inc(cnt: Int = 1): CharPointer {
        pointer += cnt
        return this
    }

    @JvmOverloads
    operator fun dec(cnt: Int = 1): CharPointer {
        pointer -= cnt
        return this
    }

    fun assign(ptr: CharPointer): CharPointer {
        seq = ptr.seq
        pointer = ptr.pointer
        readonly = ptr.readonly
        return this
    }

    fun ref(offset: Int): CharPointer {
        return CharPointer(this, offset)
    }

    fun substring(len: Int): String {
        if (end()) return ""
        val start = pointer
        val end = normalize(pointer + len)
        return CharBuffer.wrap(seq, start, end).toString()
    }

    fun strlen(): Int {
        if (end()) return 0
        for (i in pointer until seq.length) {
            if (seq[i] == '\u0000') {
                return i - pointer
            }
        }
        return seq.length - pointer
    }

    fun strncmp(str: String, len: Int): Int {
        var len = len
        if (end()) return -1
        val s = CharBuffer.wrap(seq, pointer, normalize(pointer + len)).toString()
        if (len > str.length) {
            len = str.length
        }
        return s.compareTo(str.substring(0, len))
    }

    fun strncmp(str: CharPointer, len: Int, ignoreCase: Boolean): Int {
        if (end()) return -1
        val cs1: CharSequence = CharBuffer.wrap(seq, pointer, normalize(pointer + len))
        val cs2: CharSequence = CharBuffer.wrap(str.seq, str.pointer, str.normalize(str.pointer + len))
        val l = cs1.length
        if (l != cs2.length) {
            return 1
        }
        for (i in 0 until l) {
            val c1 = cs1[i]
            val c2 = cs2[i]
            val notEqual = if (ignoreCase) c1.lowercaseChar() != c2.lowercaseChar() &&
                    c1.uppercaseChar() != c2.uppercaseChar() else c1 != c2
            if (notEqual) return 1
        }
        return 0
    }

    fun strchr(c: Char): CharPointer? {
        if (end()) {
            return null
        }
        val len = seq.length
        for (i in pointer until len) {
            val ch = seq[i]
            if (ch == '\u0000') {
                return null
            }
            if (ch == c) {
                return ref(i - pointer)
            }
        }
        return null
    }

    fun istrchr(c: Char): CharPointer? {
        var c = c
        if (end()) {
            return null
        }
        val len = seq.length
        val cc = c.uppercaseChar()
        c = c.lowercaseChar()
        for (i in pointer until len) {
            val ch = seq[i]
            if (ch == '\u0000') {
                return null
            }
            if (ch == c || ch == cc) {
                return ref(i - pointer)
            }
        }
        return null
    }

    val isNul: Boolean
        get() = charAt() == '\u0000'

    @JvmOverloads
    fun end(offset: Int = 0): Boolean {
        return pointer + offset >= seq.length
    }

    fun OP(): Int {
        return charAt().code
    }

    fun OPERAND(): CharPointer {
        return ref(3)
    }

    fun NEXT(): Int {
        return (seq[pointer + 1].code and 0xff shl 8) + (seq[pointer + 2].code and 0xff)
    }

    fun OPERAND_MIN(): Int {
        return (seq[pointer + 3].code shl 24) +
                (seq[pointer + 4].code shl 16) +
                (seq[pointer + 5].code shl 8) +
                seq[pointer + 6].code
    }

    fun OPERAND_MAX(): Int {
        return (seq[pointer + 7].code shl 24) +
                (seq[pointer + 8].code shl 16) +
                (seq[pointer + 9].code shl 8) +
                seq[pointer + 10].code
    }

    fun OPERAND_CMP(): Char {
        return seq[pointer + 7]
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is CharPointer) {
            val ptr = obj
            return ptr.seq === seq && ptr.pointer == pointer
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(seq, pointer)
    }

    fun skipWhitespaces() {
        while (CharacterClasses.isWhite(charAt())) inc()
    }

    val digits: Int
        get() {
            var res = 0
            while (Character.isDigit(charAt())) {
                res = res * 10 + (charAt() - '0')
                inc()
            }
            return res
        }

    private fun normalize(pos: Int): Int {
        return Math.min(seq.length, pos)
    }

    override fun toString(): String {
        return substring(strlen())
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
