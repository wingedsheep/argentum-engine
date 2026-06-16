package com.wingedsheep.tooling.coverage.dashboard

/**
 * Minimal, dependency-free terminal toolkit for the coverage dashboard. The `:mtgish-tooling` module
 * deliberately carries no runtime dependency beyond kotlinx-serialization (see its build.gradle.kts),
 * so rather than pull in a TUI library we drive the terminal with raw ANSI escapes and put it into
 * raw mode by shelling out to `stty` (POSIX; this tool only runs on the dev's mac/linux box).
 */

/** 256-colour ANSI styling. Cells are padded as plain text first, then wrapped, so widths stay exact. */
object Ansi {
    private const val ESC = ""
    const val RESET = "$ESC[0m"
    const val BOLD = "$ESC[1m"
    const val DIM = "$ESC[2m"
    const val REVERSE = "$ESC[7m"

    fun fg(n: Int) = "$ESC[38;5;${n}m"
    fun bg(n: Int) = "$ESC[48;5;${n}m"

    // palette (256-colour indices)
    const val GREEN = 42
    const val YELLOW = 178
    const val RED = 203
    const val BLUE = 39
    const val CYAN = 44
    const val ORANGE = 215
    const val GREY = 244
    const val DARKGREY = 240
    const val WHITE = 252

    fun style(text: String, vararg codes: String): String =
        if (codes.isEmpty()) text else codes.joinToString("") + text + RESET

    /** Truncate (with an ellipsis) or right-pad [s] to exactly [width] visible columns. */
    fun fit(s: String, width: Int): String = when {
        width <= 0 -> ""
        s.length <= width -> s + " ".repeat(width - s.length)
        width == 1 -> "…"
        else -> s.take(width - 1) + "…"
    }
}

/** A decoded key press. */
sealed interface Key {
    data object Up : Key
    data object Down : Key
    data object Left : Key
    data object Right : Key
    data object Enter : Key
    data object Tab : Key
    data object Esc : Key
    data object Backspace : Key
    data object PageUp : Key
    data object PageDown : Key
    data object Home : Key
    data object End : Key
    data object Quit : Key
    data class Char(val c: kotlin.Char) : Key
    data object Unknown : Key
}

/**
 * Raw-mode terminal: alternate screen buffer, hidden cursor, byte-level key reads. Always pair
 * [enter] with [exit] in a finally block so the user's terminal is restored even if we crash.
 */
class RawTerminal {
    private val esc = ""
    private var savedStty: String? = null

    fun enter() {
        savedStty = sh("stty -g < /dev/tty").trim().ifEmpty { null }
        sh("stty raw -echo < /dev/tty")
        print("$esc[?1049h$esc[?25l$esc[2J") // alt screen, hide cursor, clear
        System.out.flush()
    }

    fun exit() {
        print("$esc[?25h$esc[?1049l") // show cursor, leave alt screen
        System.out.flush()
        savedStty?.let { sh("stty $it < /dev/tty") }
    }

    /** (rows, cols); falls back to 24x80 if the terminal can't be queried. */
    fun size(): Pair<Int, Int> {
        val parts = sh("stty size < /dev/tty").trim().split(Regex("\\s+"))
        val rows = parts.getOrNull(0)?.toIntOrNull() ?: 24
        val cols = parts.getOrNull(1)?.toIntOrNull() ?: 80
        return rows to cols
    }

    /** Paint [lines] from the top-left, clearing each line's tail and everything below. */
    fun render(lines: List<String>) {
        val sb = StringBuilder("$esc[H")
        for (i in lines.indices) {
            sb.append(lines[i]).append("$esc[K")
            if (i < lines.lastIndex) sb.append("\r\n")
        }
        sb.append("$esc[J")
        System.out.print(sb)
        System.out.flush()
    }

    fun readKey(): Key {
        val b = System.`in`.read()
        return when (b) {
            -1, 3, 4 -> Key.Quit // EOF, Ctrl-C, Ctrl-D
            13, 10 -> Key.Enter
            9 -> Key.Tab
            127, 8 -> Key.Backspace
            27 -> readEscape()
            else -> Key.Char(b.toChar())
        }
    }

    /**
     * Discard any input bytes already sitting in the buffer. Used to drop a key-repeat backlog once a
     * held scroll key has hit the top/bottom of a list, so the next real keypress isn't queued behind it.
     */
    fun drainInput() {
        while (System.`in`.available() > 0) System.`in`.read()
    }

    /** ESC was read; an arrow/nav key follows immediately, a lone ESC means "back". */
    private fun readEscape(): Key {
        Thread.sleep(2) // give the rest of the sequence time to arrive in the input buffer
        if (System.`in`.available() == 0) return Key.Esc
        val intro = System.`in`.read().toChar()
        if (intro != '[' && intro != 'O') return Key.Esc
        return when (val c = System.`in`.read().toChar()) {
            'A' -> Key.Up
            'B' -> Key.Down
            'C' -> Key.Right
            'D' -> Key.Left
            'H' -> Key.Home
            'F' -> Key.End
            '5', '6' -> { if (System.`in`.available() > 0) System.`in`.read(); if (c == '5') Key.PageUp else Key.PageDown }
            else -> Key.Unknown
        }
    }

    companion object {
        /** Is there a controlling terminal we can switch into raw mode? */
        fun available(): Boolean = sh("test -e /dev/tty && echo yes").contains("yes")

        private fun sh(cmd: String): String = runCatching {
            val p = ProcessBuilder("/bin/sh", "-c", cmd).redirectErrorStream(true).start()
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            p.waitFor()
            out
        }.getOrDefault("")
    }
}
