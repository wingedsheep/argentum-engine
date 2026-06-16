package com.wingedsheep.tooling.coverage.dashboard

import com.wingedsheep.tooling.coverage.Scryfall
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive TUI for the mtgish coverage tooling — a navigable, two-pane dashboard over the same
 * analysis the `just coverage*` CLIs print. Left pane: every set with its three headline numbers —
 * implemented/total, free-to-add (+N), and auto-gen-whole (gN), matching the set-detail pane. Right pane:
 * the selected set's coverage breakdown (implemented / free-to-add / blocked) and the feature
 * leaderboard, then drill into the card list and a per-card capability verdict. `c` rolls every set's
 * leaderboard into one cross-set "what engine work unlocks the most cards everywhere" ranking.
 *
 * Wired into `just coverage-dashboard` (see Main.kt `dashboard` subcommand).
 */
object Dashboard {
    private enum class Mode { SETS, CARDS, CARD, CROSS, CROSS_CARDS }
    private enum class Sort { DATE, ALPHA }

    private val tty = RawTerminal()
    private lateinit var sets: List<Analyzer.SetRef>
    private var sortMode = Sort.DATE

    private var mode = Mode.SETS
    private var setSel = 0
    private var setScroll = 0
    private var cardSel = 0
    private var cardScroll = 0
    private var reqScroll = 0
    private var crossSel = 0
    private var crossScroll = 0
    private var crossCardSel = 0
    private var crossCardScroll = 0
    private var filter = "" // card-list filter (CARDS mode)
    private var setFilter = "" // set-list search (SETS mode)
    private var filtering = false
    private var cardName: String? = null
    private var cardFrom = Mode.CARDS // which mode the CARD detail was opened from (back returns here)
    private var cardCodeView = true // card detail shows generated Kotlin (vs the capability list)
    private var highlightName: String? = null // which card `highlightLines` was tokenized for
    private var highlightLines: List<List<KotlinHighlight.Tok>> = emptyList()
    private var cross: List<Analyzer.CrossRow>? = null

    fun run(args: List<String>): Int {
        Analyzer.init()
        sets = sortedSets(Analyzer.sets())
        if (sets.isEmpty()) {
            System.err.println("no sets found — implement a set or cache one with `just coverage --set <CODE>`")
            return 1
        }
        if ("--render" in args) return selfRender()
        if (!RawTerminal.available()) {
            System.err.println("coverage-dashboard needs an interactive terminal (no /dev/tty available).")
            return 1
        }
        tty.enter()
        try {
            if ("--scan" in args) { val (rows, cols) = tty.size(); scanAll(rows, cols, "Full scan — analyzing every set") }
            loop()
        } finally {
            tty.exit()
        }
        return 0
    }

    /**
     * Non-interactive smoke path (`dashboard --render`): build and print static frames at a fixed size
     * for the first set that has Scryfall data, without entering raw mode. Exercises the whole Analyzer
     * pipeline + every render builder so the dashboard can be verified outside a live terminal.
     */
    private fun selfRender(): Int {
        val rows = 34; val cols = 110
        setSel = sets.indexOfFirst { Analyzer.counts(it.code).total > 0 }.coerceAtLeast(0)
        val d = Analyzer.detail(currentCode())
        mode = Mode.SETS; printFrame("SET DETAIL", rows, cols)
        mode = Mode.CARDS; printFrame("CARD LIST", rows, cols)
        cardName = (d.cards.firstOrNull { it.gen == Analyzer.Gen.WHOLE } ?: d.cards.first()).name
        mode = Mode.CARD
        cardCodeView = true; printFrame("CARD DETAIL (Kotlin) — $cardName", rows, cols)
        cardCodeView = false; printFrame("CARD DETAIL (capabilities) — $cardName", rows, cols)
        return 0
    }

    private fun printFrame(label: String, rows: Int, cols: Int) {
        println("\n===== $label =====")
        buildFrame(rows, cols).forEach(::println)
    }

    private fun loop() {
        while (true) {
            val (rows, cols) = tty.size()
            ensureCurrentComputed(rows, cols)
            tty.render(buildFrame(rows, cols))
            val key = tty.readKey()
            val wasFiltering = filtering
            val before = scrollSnapshot()
            if (!handle(key, rows, cols)) break
            // A held scroll key that no longer moves the cursor has hit the top/bottom of the list.
            // Drop the key-repeat backlog so the next real keypress responds immediately instead of
            // draining one no-op per frame. (Skip while filtering — those keystrokes are text.)
            if (!wasFiltering && isScrollKey(key) && scrollSnapshot() == before) tty.drainInput()
        }
    }

    /** The active mode's cursor/scroll positions — compared across a keypress to detect a no-op scroll. */
    private fun scrollSnapshot(): List<Int> =
        listOf(setSel, cardSel, reqScroll, crossSel, crossCardSel)

    private fun isScrollKey(key: Key): Boolean = when (key) {
        Key.Up, Key.Down, Key.PageUp, Key.PageDown, Key.Home, Key.End -> true
        is Key.Char -> key.c == 'j' || key.c == 'k'
        else -> false
    }

    /** First visit to a set blocks on analysis (and, on the very first, the 29MB IR) — show a frame. */
    private fun ensureCurrentComputed(rows: Int, cols: Int) {
        if (mode == Mode.CROSS || mode == Mode.CROSS_CARDS) return
        val code = currentCode().ifEmpty { return }
        if (!Analyzer.isComputed(code) && Analyzer.counts(code).total > 0) {
            val note = if (Analyzer.indexLoaded()) "" else "  (loading mtgish IR, first run only)"
            tty.render(centered(rows, cols, "Analyzing ${code}…$note"))
            Analyzer.detail(code)
        }
    }

    // ============================================================ rendering

    private fun buildFrame(rows: Int, cols: Int): List<String> {
        val bodyH = max(1, rows - 2)
        val body: List<String> = if (mode == Mode.CROSS) {
            buildCross(bodyH, cols)
        } else if (mode == Mode.CROSS_CARDS) {
            buildCrossCards(bodyH, cols)
        } else {
            val leftW = max(28, min(48, cols * 2 / 5))
            val rightW = max(1, cols - leftW - 1)
            val left = buildSetList(bodyH, leftW)
            val right = when (mode) {
                Mode.SETS -> buildSetDetail(bodyH, rightW)
                Mode.CARDS -> buildCardList(bodyH, rightW)
                Mode.CARD -> buildCardDetail(bodyH, rightW)
                Mode.CROSS, Mode.CROSS_CARDS -> emptyList()
            }
            val sep = Ansi.style("│", Ansi.fg(Ansi.DARKGREY))
            (0 until bodyH).map { i ->
                left.getOrElse(i) { " ".repeat(leftW) } + sep + right.getOrElse(i) { " ".repeat(rightW) }
            }
        }
        return listOf(buildTitle(cols)) + body + listOf(buildFooter(cols))
    }

    private fun buildTitle(cols: Int): String {
        val totTotal = sets.sumOf { Analyzer.counts(it.code).total }
        val totImpl = sets.sumOf { Analyzer.counts(it.code).implemented }
        val pct = if (totTotal == 0) 0 else (totImpl * 100.0 / totTotal).roundToInt()
        // Auto-gen total sums only the sets analyzed so far (browsing a set, or pressing `c`, analyzes
        // it). The [k/n] note shows how complete the figure is; it drops once every set is analyzed.
        var autoSum = 0
        var analyzed = 0
        for (s in sets) when {
            Analyzer.isComputed(s.code) -> { analyzed++; autoSum += Analyzer.detail(s.code).autogen }
            Analyzer.counts(s.code).total == 0 -> analyzed++
        }
        val autoNote = if (analyzed < sets.size) " [$analyzed/${sets.size} sets analyzed — press c]" else ""
        val text = " mtgish coverage   ${sets.size} sets   $totImpl/$totTotal implemented ($pct%)   ·   +$autoSum auto-gen ready$autoNote"
        return Ansi.style(Ansi.fit(text, cols), Ansi.BOLD, Ansi.fg(Ansi.WHITE), Ansi.bg(Ansi.BLUE))
    }

    private fun buildFooter(cols: Int): String {
        if (filtering) {
            val label = if (mode == Mode.SETS) "search sets" else "filter cards"
            val text = if (mode == Mode.SETS) setFilter else filter
            return Ansi.style(Ansi.fit(" $label: $text█   (enter apply · esc clear)", cols), Ansi.fg(Ansi.YELLOW))
        }
        val help = when (mode) {
            Mode.SETS -> "↑↓ set · → cards · / search · s sort:${if (sortMode == Sort.DATE) "date" else "a-z"} · f scan-all · c cross · q quit"
            Mode.CARDS -> "↑↓ card · → detail · ← sets · / filter · c cross-set · q quit"
            Mode.CARD -> "↑↓ scroll · tab Kotlin/capabilities · ← cards · q quit"
            Mode.CROSS -> "↑↓ capability · → cards it unlocks · ← back · q quit"
            Mode.CROSS_CARDS -> "↑↓ card · → detail · ← capabilities · q quit"
        }
        return Ansi.style(Ansi.fit(" $help", cols), Ansi.fg(Ansi.GREY))
    }

    private fun buildSetList(h: Int, w: Int): List<String> {
        val shown = shownSets()
        if (setSel >= shown.size) setSel = max(0, shown.size - 1)
        val lines = ArrayList<String>()
        val title = if (setFilter.isBlank()) " SETS (${sets.size})" else " SETS (${shown.size}/${sets.size}) · '$setFilter'"
        val legend = "impl/tot +free gWhole"
        // Right-align the column legend when there's room; drop it on a narrow pane.
        val header = if (w >= title.length + legend.length + 2) title + " ".repeat(w - title.length - legend.length - 1) + legend else title
        lines.add(Ansi.style(Ansi.fit(header, w), Ansi.BOLD, Ansi.fg(Ansi.WHITE), Ansi.bg(Ansi.DARKGREY)))
        val listH = h - 1
        setScroll = clampScroll(setSel, setScroll, listH, shown.size)
        for (row in 0 until listH) {
            val idx = setScroll + row
            if (idx >= shown.size) { lines.add(" ".repeat(w)); continue }
            val s = shown[idx]
            val c = Analyzer.counts(s.code)
            val computed = Analyzer.isComputed(s.code)
            val d = if (computed && c.total > 0) Analyzer.detail(s.code) else null
            // Right column, mirroring the set-detail pane's three headline numbers, all out of `total`:
            //   implemented/total · "+F" free-to-add (no engine work) · "gG" auto-gen whole (incl. impl).
            // The free/gen figures need the set analyzed (browsing or `c`); they read "+?"/"g?" until then.
            val free = when { c.total == 0 -> ""; d != null -> "+${d.free}"; else -> "+?" }
            val gen = when { c.total == 0 -> ""; d != null -> "g${d.genWhole}"; else -> "g?" }
            val stats = if (c.total == 0) "—" else "${c.implemented}/${c.total} $free $gen"
            val left = " ${s.code.padEnd(4)} ${year(s)} ${s.name}"
            val leftW = max(0, w - stats.length - 1)
            val plain = Ansi.fit(left, leftW) + " " + stats
            lines.add(
                when {
                    idx == setSel && mode == Mode.SETS -> Ansi.style(Ansi.fit(plain, w), Ansi.REVERSE)
                    idx == setSel -> Ansi.style(Ansi.fit(plain, w), Ansi.bg(238))
                    c.total == 0 -> Ansi.style(Ansi.fit(plain, w), Ansi.fg(Ansi.DARKGREY))
                    else -> Ansi.fit(left, leftW) + " ${c.implemented}/${c.total} " +
                        Ansi.style(free, Ansi.fg(Ansi.BLUE)) + " " +
                        Ansi.style(gen, Ansi.fg(if (computed) Ansi.GREEN else Ansi.DARKGREY))
                }
            )
        }
        return lines
    }

    private fun buildSetDetail(h: Int, w: Int): List<String> {
        val lines = ArrayList<String>()
        fun add(s: String, vararg st: String) { lines.add(Ansi.style(Ansi.fit(s, w), *st)) }
        val ref = currentSet() ?: return padTo(lines, h, w)
        val c = Analyzer.counts(ref.code)
        if (c.total == 0) {
            add(" ${ref.code} — ${ref.name} (${year(ref)})", Ansi.BOLD, Ansi.fg(Ansi.WHITE))
            add("")
            add(" No Scryfall data cached for this set.", Ansi.fg(Ansi.GREY))
            add(" Press r to fetch it (network), or run:", Ansi.fg(Ansi.GREY))
            add("   just coverage --set ${ref.code}", Ansi.fg(Ansi.CYAN))
            return padTo(lines, h, w)
        }
        val d = Analyzer.detail(ref.code)
        fun stat(label: String, n: Int) = " ${label.padEnd(12)} ${n.toString().padStart(5)}  ${pct(n, d.total)}"
        add(" ${d.code} — ${d.name} (${year(ref)})", Ansi.BOLD, Ansi.fg(Ansi.WHITE))
        add(" ${d.total} cards", Ansi.fg(Ansi.GREY))
        add("")
        add(stat("Implemented", d.implemented), Ansi.fg(Ansi.GREEN))
        // auto-gen coverage spans ALL cards (implemented included) — meaningful even at 100% implemented.
        add(stat("Auto-gen", d.genWhole) + "  whole ${d.genWhole} · scaf ${d.genScaffold} · blk ${d.genBlocked}", Ansi.fg(Ansi.CYAN))
        add(stat("Free to add", d.free) + "  AUTO ${d.autogen} · SCAFF ${d.scaffold}", Ansi.fg(Ansi.BLUE))
        add(stat("Blocked", d.blocked), Ansi.fg(Ansi.RED))
        if (d.unmatched > 0) add(stat("Unmatched", d.unmatched), Ansi.fg(Ansi.DARKGREY))
        d.fidelity?.let { f ->
            add("")
            add(" fidelity (vs golden): AUTO ${f.auto} · SCAFFOLD ${f.scaffold} · MISS ${f.miss} · recall ${f.avgRecall.roundToInt()}%", Ansi.fg(Ansi.GREY))
        }
        add("")
        add(" Feature leaderboard — engine work ranked", Ansi.BOLD, Ansi.fg(Ansi.WHITE))
        add(" by # blocked cards it would unlock:", Ansi.BOLD, Ansi.fg(Ansi.WHITE))
        val room = h - lines.size
        if (d.leaderboard.isEmpty()) {
            add(if (d.blocked == 0) " — nothing blocked, every missing card is free!" else " —", Ansi.fg(Ansi.GREY))
        } else {
            for (r in d.leaderboard.take(room)) {
                val gap = r.verdict == "MISSING"
                val tag = if (gap) "GAP" else "?? "
                add(" [$tag] x${r.count.toString().padEnd(3)} ${r.disc} = ${r.value}", Ansi.fg(if (gap) Ansi.ORANGE else Ansi.GREY))
            }
        }
        return padTo(lines, h, w)
    }

    private fun filteredCards(d: Analyzer.Detail): List<Analyzer.CardVerdict> =
        if (filter.isBlank()) d.cards else d.cards.filter { it.name.contains(filter, ignoreCase = true) }

    private fun buildCardList(h: Int, w: Int): List<String> {
        val d = Analyzer.detail(currentCode())
        val cards = filteredCards(d)
        if (cardSel >= cards.size) cardSel = max(0, cards.size - 1)
        val lines = ArrayList<String>()
        val head = " ${d.code} cards (${cards.size}${if (filter.isNotBlank()) " · '$filter'" else ""})"
        lines.add(Ansi.style(Ansi.fit(head, w), Ansi.BOLD, Ansi.fg(Ansi.WHITE), Ansi.bg(Ansi.DARKGREY)))
        val listH = h - 1
        cardScroll = clampScroll(cardSel, cardScroll, listH, cards.size)
        for (row in 0 until listH) {
            val idx = cardScroll + row
            if (idx >= cards.size) { lines.add(" ".repeat(w)); continue }
            val cv = cards[idx]
            // marker = implemented? ✓ : new; colour = the generator's verdict (so an implemented card
            // the generator can't reproduce shows a red ✓ — a hand-authored card needing engine work).
            val plain = " ${marker(cv)} ${cv.name}"
            lines.add(
                if (idx == cardSel && mode == Mode.CARDS) Ansi.style(Ansi.fit(plain, w), Ansi.REVERSE)
                else Ansi.style(Ansi.fit(plain, w), Ansi.fg(genColor(cv.gen)))
            )
        }
        return lines
    }

    private fun buildCardDetail(h: Int, w: Int): List<String> {
        val name = cardName ?: return padTo(ArrayList(), h, w)
        val code = currentCode()
        val rep = Analyzer.cardReport(name)
        val cv = Analyzer.verdictFor(code, name)
        val lines = ArrayList<String>()
        fun add(s: String, vararg st: String) { lines.add(Ansi.style(Ansi.fit(s, w), *st)) }
        val impl = if (cv?.implemented == true) "implemented · " else ""
        add(" ${rep.name}", Ansi.BOLD, Ansi.fg(Ansi.WHITE))
        when (cv?.gen) {
            Analyzer.Gen.WHOLE -> add(" ${impl}auto-gen: WHOLE — generator renders the whole card", Ansi.fg(Ansi.GREEN))
            Analyzer.Gen.SCAFFOLD -> {
                val r = Analyzer.cardRender(code, name)
                val note = r?.let { " — ${(it.renderableFraction * 100).roundToInt()}% renderable, ${it.holes.size} part${if (it.holes.size == 1) "" else "s"} to hand-wire" } ?: " — covered, structure needs hand-wiring"
                add(" ${impl}auto-gen: SCAFFOLD$note", Ansi.fg(Ansi.YELLOW))
            }
            Analyzer.Gen.BLOCKED -> add(" ${impl}auto-gen: BLOCKED — needs engine work", Ansi.fg(Ansi.RED))
            else -> add(" ${impl}not in mtgish IR (name join / Un-set / too new)", Ansi.fg(Ansi.GREY))
        }
        add(if (cardCodeView) " ▸ Kotlin    ·   capabilities" else "   Kotlin    ·  ▸ capabilities", Ansi.fg(Ansi.CYAN))
        add("")
        if (cardCodeView) appendCardSource(lines, code, name, rep, h, w, ::add) else appendCapabilities(lines, rep, h, ::add)
        return padTo(lines, h, w)
    }

    private fun appendCardSource(lines: MutableList<String>, code: String, name: String, rep: Analyzer.CardReport, h: Int, w: Int, add: (String, Array<out String>) -> Unit) {
        // Missing mappings first (fixed header) — the capabilities the bridge can't yet express, which
        // are exactly why a BLOCKED card's generated source below is a scaffold rather than whole.
        val missing = rep.reqs.filter { it.verdict == "MISSING" || it.verdict == "UNMAPPED" }
        if (missing.isEmpty()) {
            add(if (rep.unmatched) "" else " ✓ all capabilities mapped", arrayOf(Ansi.fg(Ansi.GREEN)))
        } else {
            add(" ⚠ missing mappings (${missing.size}):", arrayOf(Ansi.BOLD, Ansi.fg(Ansi.ORANGE)))
            for (m in missing.take(4)) {
                val tag = if (m.verdict == "MISSING") "GAP" else "?? "
                add("   [$tag] ${m.disc} = ${m.value}", arrayOf(Ansi.fg(Ansi.ORANGE)))
            }
            if (missing.size > 4) add("   +${missing.size - 4} more — tab for full capability list", arrayOf(Ansi.fg(Ansi.DARKGREY)))
        }
        // Located holes — the parts the emitter could not render (the `// TODO(hole)` lines below). This
        // is the per-part "still to implement" list, distinct from the capability gaps above: a card can
        // have every capability mapped yet still hole an ability whose STRUCTURE the emitter can't recover.
        val render = Analyzer.cardRender(code, name)
        if (render != null && render.holes.isNotEmpty()) {
            add(" ⌗ parts to hand-wire (${render.holes.size}, ${(render.renderableFraction * 100).roundToInt()}% renderable):", arrayOf(Ansi.BOLD, Ansi.fg(Ansi.YELLOW)))
            for (hole in render.holes.take(4)) add("   • $hole", arrayOf(Ansi.fg(Ansi.YELLOW)))
            if (render.holes.size > 4) add("   +${render.holes.size - 4} more", arrayOf(Ansi.fg(Ansi.DARKGREY)))
        }
        add(" ── generated cardDef ${"─".repeat(max(0, w - 18))}", arrayOf(Ansi.fg(Ansi.DARKGREY)))
        val src = Analyzer.cardSource(code, name)
        if (src == null) { add(" (no generated source — card absent from mtgish IR)", arrayOf(Ansi.fg(Ansi.GREY))); return }
        val key = "$code/$name"
        if (highlightName != key) { highlightLines = KotlinHighlight.highlight(src); highlightName = key }
        val room = h - lines.size
        reqScroll = reqScroll.coerceIn(0, max(0, highlightLines.size - room))
        for (toks in highlightLines.drop(reqScroll).take(room)) lines.add(KotlinHighlight.renderLine(toks, w))
        if (highlightLines.size > room) lines[lines.lastIndex] = Ansi.style(Ansi.fit(" … ${highlightLines.size - reqScroll - room} more lines (↑↓)", w), Ansi.fg(Ansi.DARKGREY))
    }

    private fun appendCapabilities(lines: MutableList<String>, rep: Analyzer.CardReport, h: Int, add: (String, Array<out String>) -> Unit) {
        if (rep.implementedIn.isNotEmpty()) add(" implemented in: ${rep.implementedIn.sorted().joinToString(", ")}", arrayOf(Ansi.fg(Ansi.GREY)))
        add(" Required capabilities:", arrayOf(Ansi.BOLD, Ansi.fg(Ansi.WHITE)))
        val ordered = rep.reqs.sortedBy { VERDICT_ORDER[it.verdict] ?: 9 }
        val room = h - lines.size
        reqScroll = reqScroll.coerceIn(0, max(0, ordered.size - room))
        if (ordered.isEmpty()) { add(" — none extracted", arrayOf(Ansi.fg(Ansi.GREY))); return }
        for (r in ordered.drop(reqScroll).take(room)) {
            val mark = MARK[r.verdict] ?: "?? "
            val arrow = if (r.detail.isNotEmpty()) " → ${r.detail}" else ""
            add(" [$mark] ${r.disc} ${r.value}$arrow", arrayOf(Ansi.fg(verdictColor(r.verdict))))
        }
    }

    private fun buildCross(h: Int, w: Int): List<String> {
        val rows = cross ?: emptyList()
        if (crossSel >= rows.size) crossSel = max(0, rows.size - 1)
        val lines = ArrayList<String>()
        lines.add(Ansi.style(Ansi.fit(" CROSS-SET CAPABILITY INDEX — engine work ranked by # blocked cards across all sets (${rows.size} capabilities)", w), Ansi.BOLD, Ansi.fg(Ansi.WHITE), Ansi.bg(Ansi.DARKGREY)))
        val listH = h - 1
        crossScroll = clampScroll(crossSel, crossScroll, listH, rows.size)
        for (row in 0 until listH) {
            val idx = crossScroll + row
            if (idx >= rows.size) { lines.add(" ".repeat(w)); continue }
            val r = rows[idx]
            val gap = r.verdict == "MISSING"
            val tag = if (gap) "GAP" else "?? "
            val plain = " [$tag] ${("x" + r.count).padEnd(6)} ${(r.sets.toString() + " sets").padEnd(9)} ${r.disc} = ${r.value}"
            lines.add(
                if (idx == crossSel) Ansi.style(Ansi.fit(plain, w), Ansi.REVERSE)
                else Ansi.style(Ansi.fit(plain, w), Ansi.fg(if (gap) Ansi.ORANGE else Ansi.GREY))
            )
        }
        return lines
    }

    /** Drill-down of a single cross-set capability: the blocked cards adding that mapping would unlock. */
    private fun buildCrossCards(h: Int, w: Int): List<String> {
        val row = cross?.getOrNull(crossSel)
        val lines = ArrayList<String>()
        if (row == null) { lines.add(Ansi.fit(" no capability selected", w)); return padTo(lines, h, w) }
        val cards = row.cards
        if (crossCardSel >= cards.size) crossCardSel = max(0, cards.size - 1)
        val gap = row.verdict == "MISSING"
        val tag = if (gap) "GAP" else "?? "
        val noun = if (cards.size == 1) "card" else "cards"
        val head = " [$tag] ${row.disc} = ${row.value}  —  ${cards.size} $noun it would unlock across ${row.sets} set${if (row.sets == 1) "" else "s"}"
        lines.add(Ansi.style(Ansi.fit(head, w), Ansi.BOLD, Ansi.fg(Ansi.WHITE), Ansi.bg(Ansi.DARKGREY)))
        val listH = h - 1
        crossCardScroll = clampScroll(crossCardSel, crossCardScroll, listH, cards.size)
        for (rowIdx in 0 until listH) {
            val idx = crossCardScroll + rowIdx
            if (idx >= cards.size) { lines.add(" ".repeat(w)); continue }
            val cc = cards[idx]
            val plain = " ${cc.set.padEnd(4)} ${cc.name}"
            lines.add(
                if (idx == crossCardSel) Ansi.style(Ansi.fit(plain, w), Ansi.REVERSE)
                else Ansi.style(Ansi.fit(plain, w), Ansi.fg(if (gap) Ansi.ORANGE else Ansi.GREY))
            )
        }
        return lines
    }

    // ============================================================ input

    private fun handle(key: Key, rows: Int, cols: Int): Boolean {
        if (filtering) {
            val inSets = mode == Mode.SETS
            when (key) {
                Key.Enter -> filtering = false
                Key.Esc -> { if (inSets) setFilter = "" else filter = ""; filtering = false; resetSelection(inSets) }
                Key.Backspace -> { if (inSets) { if (setFilter.isNotEmpty()) setFilter = setFilter.dropLast(1) } else { if (filter.isNotEmpty()) filter = filter.dropLast(1) }; resetSelection(inSets) }
                is Key.Char -> { if (inSets) setFilter += key.c else filter += key.c; resetSelection(inSets) }
                else -> {}
            }
            return true
        }
        if (key == Key.Quit || key == Key.Char('q')) return false
        if (key == Key.Char('c')) { enterCross(rows, cols); return true }
        val pageStep = max(1, rows - 4)
        when (mode) {
            Mode.SETS -> {
                val shown = shownSets()
                when (key) {
                    Key.Up, Key.Char('k') -> setSel = max(0, setSel - 1)
                    Key.Down, Key.Char('j') -> setSel = min(max(0, shown.lastIndex), setSel + 1)
                    Key.Home -> setSel = 0
                    Key.End -> setSel = max(0, shown.lastIndex)
                    Key.PageUp -> setSel = max(0, setSel - pageStep)
                    Key.PageDown -> setSel = min(max(0, shown.lastIndex), setSel + pageStep)
                    Key.Enter, Key.Right, Key.Char('l') -> if (Analyzer.counts(currentCode()).total > 0) { mode = Mode.CARDS; cardSel = 0; cardScroll = 0; filter = "" }
                    Key.Char('/') -> filtering = true
                    Key.Char('s') -> toggleSort()
                    Key.Char('f') -> scanAll(rows, cols, "Full scan — analyzing every set")
                    Key.Char('r') -> refresh(rows, cols)
                    else -> {}
                }
            }
            Mode.CARDS -> {
                val cards = filteredCards(Analyzer.detail(currentCode()))
                when (key) {
                    Key.Up, Key.Char('k') -> cardSel = max(0, cardSel - 1)
                    Key.Down, Key.Char('j') -> cardSel = min(max(0, cards.lastIndex), cardSel + 1)
                    Key.Home -> cardSel = 0
                    Key.End -> cardSel = max(0, cards.lastIndex)
                    Key.PageUp -> cardSel = max(0, cardSel - pageStep)
                    Key.PageDown -> cardSel = min(max(0, cards.lastIndex), cardSel + pageStep)
                    Key.Enter, Key.Right, Key.Char('l') -> if (cards.isNotEmpty()) { cardName = cards[cardSel.coerceIn(0, cards.lastIndex)].name; cardFrom = Mode.CARDS; mode = Mode.CARD; cardCodeView = true; reqScroll = 0 }
                    Key.Esc, Key.Left, Key.Char('h') -> mode = Mode.SETS
                    Key.Char('/') -> filtering = true
                    else -> {}
                }
            }
            Mode.CARD -> when (key) {
                Key.Up, Key.Char('k') -> reqScroll = max(0, reqScroll - 1)
                Key.Down, Key.Char('j') -> reqScroll += 1
                Key.PageUp -> reqScroll = max(0, reqScroll - pageStep)
                Key.PageDown -> reqScroll += pageStep
                Key.Home -> reqScroll = 0
                Key.Tab, Key.Char('t') -> { cardCodeView = !cardCodeView; reqScroll = 0 }
                Key.Esc, Key.Left, Key.Char('h') -> mode = cardFrom
                else -> {}
            }
            Mode.CROSS -> {
                val n = cross?.size ?: 0
                when (key) {
                    Key.Up, Key.Char('k') -> crossSel = max(0, crossSel - 1)
                    Key.Down, Key.Char('j') -> crossSel = min(max(0, n - 1), crossSel + 1)
                    Key.Home -> crossSel = 0
                    Key.End -> crossSel = max(0, n - 1)
                    Key.PageUp -> crossSel = max(0, crossSel - pageStep)
                    Key.PageDown -> crossSel = min(max(0, n - 1), crossSel + pageStep)
                    Key.Enter, Key.Right, Key.Char('l') -> if ((cross?.getOrNull(crossSel)?.cards?.isNotEmpty()) == true) { mode = Mode.CROSS_CARDS; crossCardSel = 0; crossCardScroll = 0 }
                    Key.Esc, Key.Left, Key.Char('h') -> mode = Mode.SETS
                    else -> {}
                }
            }
            Mode.CROSS_CARDS -> {
                val cards = cross?.getOrNull(crossSel)?.cards ?: emptyList()
                when (key) {
                    Key.Up, Key.Char('k') -> crossCardSel = max(0, crossCardSel - 1)
                    Key.Down, Key.Char('j') -> crossCardSel = min(max(0, cards.lastIndex), crossCardSel + 1)
                    Key.Home -> crossCardSel = 0
                    Key.End -> crossCardSel = max(0, cards.lastIndex)
                    Key.PageUp -> crossCardSel = max(0, crossCardSel - pageStep)
                    Key.PageDown -> crossCardSel = min(max(0, cards.lastIndex), crossCardSel + pageStep)
                    // Open the card's detail in the context of the set it lives in; back returns here.
                    Key.Enter, Key.Right, Key.Char('l') -> cards.getOrNull(crossCardSel)?.let { cc ->
                        setSel = shownSets().indexOfFirst { it.code == cc.set }.coerceAtLeast(0)
                        cardName = cc.name; cardFrom = Mode.CROSS_CARDS; mode = Mode.CARD; cardCodeView = true; reqScroll = 0
                    }
                    Key.Esc, Key.Left, Key.Char('h') -> mode = Mode.CROSS
                    else -> {}
                }
            }
        }
        return true
    }

    /**
     * Analyze every set in one pass (with a progress bar): fills in each set's `+N` auto-gen count,
     * completes the global auto-gen total, and builds the cross-set index — so `c` is then instant.
     * Memoized via [Analyzer], so re-running is a no-op until a set is refreshed.
     */
    private fun scanAll(rows: Int, cols: Int, label: String) {
        if (cross != null) return
        cross = Analyzer.crossSet { done, total -> tty.render(progress(rows, cols, label, done, total)) }
    }

    private fun enterCross(rows: Int, cols: Int) {
        scanAll(rows, cols, "Building cross-set index — analyzing every set")
        mode = Mode.CROSS; crossSel = 0; crossScroll = 0
    }

    private fun refresh(rows: Int, cols: Int) {
        val code = currentCode().ifEmpty { return }
        tty.render(centered(rows, cols, "Fetching $code from Scryfall…"))
        runCatching { Scryfall.loadCanonical(code, forceRefresh = true) }
        Analyzer.invalidate(code)
        cross = null
    }

    // ============================================================ helpers

    private val VERDICT_ORDER = mapOf("UNMAPPED" to 0, "MISSING" to 1, "ok" to 2, "composed" to 3, "supported" to 4, "ignore" to 5)
    private val MARK = mapOf("ok" to "ok ", "composed" to "cmp", "supported" to "sup", "ignore" to "-- ", "MISSING" to "GAP", "UNMAPPED" to "?? ")

    private fun verdictColor(v: String) = when (v) {
        "ok", "composed", "supported" -> Ansi.GREEN
        "ignore" -> Ansi.DARKGREY
        "MISSING" -> Ansi.ORANGE
        else -> Ansi.GREY
    }

    private fun marker(cv: Analyzer.CardVerdict): String = when {
        cv.implemented -> "✓"
        cv.gen == Analyzer.Gen.WHOLE -> "+"
        cv.gen == Analyzer.Gen.SCAFFOLD -> "~"
        cv.gen == Analyzer.Gen.BLOCKED -> "✗"
        else -> "?"
    }

    private fun genColor(g: Analyzer.Gen) = when (g) {
        Analyzer.Gen.WHOLE -> Ansi.GREEN
        Analyzer.Gen.SCAFFOLD -> Ansi.YELLOW
        Analyzer.Gen.BLOCKED -> Ansi.RED
        Analyzer.Gen.NONE -> Ansi.DARKGREY
    }

    private fun pct(n: Int, total: Int) = if (total == 0) "" else "${(n * 100.0 / total).roundToInt()}%"

    /** Sets matching the current set search (by code or name); the full list when the search is blank. */
    private fun shownSets(): List<Analyzer.SetRef> =
        if (setFilter.isBlank()) sets
        else sets.filter { it.code.contains(setFilter, true) || it.name.contains(setFilter, true) }

    private fun currentSet(): Analyzer.SetRef? = shownSets().getOrNull(setSel)
    private fun currentCode(): String = currentSet()?.code ?: ""

    private fun resetSelection(inSets: Boolean) {
        if (inSets) { setSel = 0; setScroll = 0 } else { cardSel = 0; cardScroll = 0 }
    }
    private fun year(s: Analyzer.SetRef): String = s.released?.takeIf { it.length >= 4 }?.substring(0, 4) ?: "????"

    /** DATE = newest first (undated sets last); ALPHA = by set code. Both tie-break on code. */
    private fun sortedSets(list: List<Analyzer.SetRef>): List<Analyzer.SetRef> = when (sortMode) {
        Sort.DATE -> list.sortedWith(compareByDescending<Analyzer.SetRef> { it.released ?: "" }.thenBy { it.code })
        Sort.ALPHA -> list.sortedBy { it.code }
    }

    /** Flip the sort and keep the cursor on the same set (within the current search view). */
    private fun toggleSort() {
        val current = currentCode()
        sortMode = if (sortMode == Sort.DATE) Sort.ALPHA else Sort.DATE
        sets = sortedSets(sets)
        setSel = shownSets().indexOfFirst { it.code == current }.coerceAtLeast(0)
        setScroll = 0
    }

    /** Keep [sel] inside the visible window of [height] rows over [size] items, returning new scroll. */
    private fun clampScroll(sel: Int, scroll: Int, height: Int, size: Int): Int {
        if (height <= 0 || size == 0) return 0
        var s = scroll
        if (sel < s) s = sel
        if (sel >= s + height) s = sel - height + 1
        return s.coerceIn(0, max(0, size - height))
    }

    private fun padTo(lines: MutableList<String>, h: Int, w: Int): List<String> {
        while (lines.size < h) lines.add(" ".repeat(w))
        return if (lines.size > h) lines.subList(0, h) else lines
    }

    private fun centered(rows: Int, cols: Int, msg: String): List<String> {
        val mid = rows / 2
        return (0 until rows).map { if (it == mid) Ansi.fit(" ".repeat(max(0, (cols - msg.length) / 2)) + msg, cols) else " ".repeat(cols) }
    }

    private fun progress(rows: Int, cols: Int, label: String, done: Int, total: Int): List<String> {
        val mid = rows / 2
        val barW = min(40, cols - 10)
        val filled = if (total == 0) 0 else (barW * done / total)
        val bar = "█".repeat(filled) + "░".repeat(max(0, barW - filled))
        return (0 until rows).map {
            when (it) {
                mid - 1 -> Ansi.fit(" ".repeat(max(0, (cols - label.length) / 2)) + label, cols)
                mid -> Ansi.fit(" ".repeat(max(0, (cols - barW - 8) / 2)) + "$bar $done/$total", cols)
                else -> " ".repeat(cols)
            }
        }
    }
}
