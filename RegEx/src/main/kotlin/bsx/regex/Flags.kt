/*
 * Taken from IdeaVim
 * https://github.com/JetBrains/ideavim/blob/master/vim-engine/src/main/kotlin/com/maddyhome/idea/vim/regexp/Flags.kt
 * Original license: See bottom.
 */
package bsx.regex

class Flags {
    private var flags: Int

    constructor() {
        flags = 0
    }

    constructor(flags: Int) {
        this.flags = flags
    }

    fun get(): Int {
        return flags
    }

    fun isSet(flag: Int): Boolean {
        return flags and flag != 0
    }

    fun allSet(flags: Int): Boolean {
        return this.flags and flags == flags
    }

    fun init(flags: Int): Int {
        this.flags = flags
        return this.flags
    }

    fun set(flags: Int): Int {
        this.flags = this.flags or flags
        return this.flags
    }

    fun unset(flags: Int): Int {
        this.flags = this.flags and flags.inv()
        return this.flags
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
