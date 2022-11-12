package bsx.regex

// Some methods that do nothing to make RegExp work
object injector {
    object messages {
        fun showStatusBarMessage(any: Any?): Unit {}
        fun message(key: Any): String? = null
        fun message(key: Any, value: Any): String? = null
        fun message(key: Any, value1: Any, value2: Any): String? = null
    }
    object engineEditorHelper {
        fun getLineBuffer(buffer: VimEditor, num: Int): StringBuffer {
            return if (num == 0) buffer.content else StringBuffer()
        }
    }
}
