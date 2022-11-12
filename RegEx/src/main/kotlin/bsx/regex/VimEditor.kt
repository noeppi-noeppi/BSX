package bsx.regex

// Minimal single line VimEditor class to make RegExp work.
class VimEditor(val content: StringBuffer) {
    
    constructor(text: String) : this(StringBuffer(text))
    
    fun currentCaret(): VimCaret = VimCaret
    fun lineCount(): Int = 1
    override fun toString(): String = content.toString()
}

object VimCaret {
    fun getLogicalPosition(): VimCaretPos = VimCaretPos
}

object VimCaretPos {
    val line = 0
    val column =0
}
