package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.bridge.Bridge
import com.wingedsheep.tooling.coverage.bridge.MappingEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/**
 * Coverage probe (port of probe.py Part 3 + the three lenses) — predicts which cards the engine
 * could support with no new engine work ("coverable-now") vs. blocked on a missing capability.
 * TOOLING ONLY — never loads a card; ground truth stays authored DSL + passing scenario test.
 */
object Probe {
    data class Req(val disc: String, val value: String, val verdict: String, val detail: String)
    data class Blocker(val disc: String, val value: String, val verdict: String)
    data class Analysis(val coverable: Boolean, val reqs: List<Req>, val blockers: List<Blocker>)

    fun analyze(card: JsonObject, effects: Set<String>, keywords: Set<String>): Analysis {
        val tags = Counter<Pair<String, String>>()
        Mtgish.extractTags(card["Rules"] ?: JsonArray(emptyList()), tags)
        val reqs = mutableListOf<Req>()
        val blockers = mutableListOf<Blocker>()
        for ((disc, value) in tags.keys) {
            val entry = Bridge.entry(disc, value)
            if (entry == null) {
                // Principled fallback: a tag that IS a Keyword enum member is covered (registry-validated,
                // so envelopes like Activated / SpellActions stay correctly unmapped).
                val kw = pascalToUpperSnake(value)
                if (kw in keywords) {
                    reqs.add(Req(disc, value, "ok", "$kw (keyword auto)"))
                    continue
                }
                reqs.add(Req(disc, value, "UNMAPPED", ""))
                blockers.add(Blocker(disc, value, "UNMAPPED"))
                continue
            }
            when (entry) {
                is MappingEntry.Effect -> {
                    val ok = entry.tag in effects
                    reqs.add(Req(disc, value, if (ok) "ok" else "MISSING", entry.tag))
                    if (!ok) blockers.add(Blocker(disc, value, "MISSING"))
                }
                is MappingEntry.Keyword -> {
                    val ok = entry.tag in keywords
                    reqs.add(Req(disc, value, if (ok) "ok" else "MISSING", entry.tag))
                    if (!ok) blockers.add(Blocker(disc, value, "MISSING"))
                }
                else -> reqs.add(Req(disc, value, entry.kind, entry.note ?: ""))
            }
        }
        return Analysis(blockers.isEmpty(), reqs, blockers)
    }

    // ---------------------------------------------------------------------------
    // Modes
    // ---------------------------------------------------------------------------
    private val VERDICT_ORDER = mapOf("UNMAPPED" to 0, "MISSING" to 1, "ok" to 2, "composed" to 3, "supported" to 4, "ignore" to 5)
    private val MARK = mapOf("ok" to "ok  ", "composed" to "comp", "supported" to "supp", "ignore" to "--  ", "MISSING" to "GAP ", "UNMAPPED" to "??  ")

    private fun modeCard(name: String, effects: Set<String>, keywords: Set<String>): Int {
        val idx = Mtgish.loadMtgishIndex(setOf(name))
        val card = idx[Cards.front(name)]
            ?: idx.entries.firstOrNull { it.key.lowercase() == Cards.front(name).lowercase() }?.value
        if (card == null) {
            println("'$name': not found in mtgish IR (name mismatch, or an Un-set/excluded card)")
            return 1
        }
        val (coverable, reqs, _) = analyze(card, effects, keywords)
        println("Card: ${card["Name"].asStr()}")
        println("Verdict: ${if (coverable) "COVERABLE-NOW (no engine work)" else "BLOCKED (needs engine work)"}\n")
        println("Required capabilities (from mtgish IR):")
        for (r in reqs.sortedBy { VERDICT_ORDER[it.verdict] ?: 9 }) {
            val mark = MARK[r.verdict] ?: "??  "
            val arrow = if (r.detail.isNotEmpty()) " -> ${r.detail}" else ""
            println("  [$mark] ${r.disc.padEnd(13)} ${r.value}$arrow")
        }
        val impl = Cards.implementedNamesForCard(card["Name"].asStr() ?: "")
        if (impl.isNotEmpty()) println("\nAlready implemented in: ${impl.sorted().joinToString(", ")}")
        return if (coverable) 0 else 2
    }

    private fun modeSet(code: String, effects: Set<String>, keywords: Set<String>, show: String?, refresh: Boolean): Int {
        val (draft, extra) = Cards.canonicalNames(code, refresh)
        if (draft == null || extra == null) {
            println("no Scryfall data for set ${code.uppercase()} (run: just card-status --set ${code.uppercase()})")
            return 1
        }
        val canonical = draft + extra
        val impl = Cards.implementedNames(code)
        val idx = Mtgish.loadMtgishIndex(canonical)

        val implList = mutableListOf<String>()
        val free = mutableListOf<String>()
        val blocked = mutableListOf<String>()
        val unmatched = mutableListOf<String>()
        val leaderboard = Counter<Pair<String, String>>()
        val leaderboardKind = HashMap<Pair<String, String>, String>()
        val blockedDetail = HashMap<String, List<Blocker>>()
        for (name in canonical.sorted()) {
            if (name in impl) { implList.add(name); continue }
            val card = idx[name]
            if (card == null) { unmatched.add(name); continue }
            val (coverable, _, blockers) = analyze(card, effects, keywords)
            if (coverable) {
                free.add(name)
            } else {
                blocked.add(name)
                blockedDetail[name] = blockers
                for (b in blockers) {
                    leaderboard.add(b.disc to b.value)
                    leaderboardKind[b.disc to b.value] = b.verdict
                }
            }
        }

        val total = canonical.size
        println("== ${code.uppercase()} — $total cards (${draft.size} draft / ${extra.size} extra) ==")
        println("  implemented:            ${implList.size}")
        println("  FREE to implement now:  ${free.size}   (missing, fully coverable)")
        println("  blocked on engine work: ${blocked.size}")
        if (unmatched.isNotEmpty()) println("  (unmatched in mtgish:   ${unmatched.size} — name join / Un-set)")

        if (!leaderboard.isEmpty) {
            println("\nFeature leaderboard — missing capability ranked by # blocked cards it unlocks:")
            for ((key, n) in leaderboard.mostCommon(20)) {
                val (disc, value) = key
                val tag = if (leaderboardKind[key] == "MISSING") "GAP " else "??  "
                val label = if (tag == "GAP ") "engine capability absent" else "unmapped — triage"
                println("  [$tag] x${n.toString().padEnd(4)} $disc = $value   ($label)")
            }
        }
        if (show == "free" || show == "all") {
            println("\nFREE to implement now (${free.size}):")
            free.forEach { println("  + $it") }
        }
        if (show == "blocked" || show == "all") {
            println("\nBLOCKED (${blocked.size}):")
            blocked.forEach { n -> println("  - $n   [${blockedDetail[n]!!.joinToString(",") { it.value }}]") }
        }
        return 0
    }

    private fun modeCalibrate(code: String, effects: Set<String>, keywords: Set<String>): Int {
        val names = Cards.implementedNames(code)
        val idx = Mtgish.loadMtgishIndex(names)
        var coverableN = 0
        val holes = Counter<Pair<String, String>>()
        val blocked = mutableListOf<Pair<String, List<Blocker>>>()
        for ((name, card) in idx.entries.sortedBy { it.key }) {
            val (ok, _, blockers) = analyze(card, effects, keywords)
            if (ok) {
                coverableN++
            } else {
                blocked.add(name to blockers)
                for (b in blockers) holes.add(b.disc to b.value)
            }
        }
        val total = idx.size
        val recall = if (total > 0) coverableN.toDouble() / total * 100 else 0.0
        println("CALIBRATION ${code.uppercase()}: $coverableN/$total implemented cards classify coverable-now = ${String.format(Locale.ROOT, "%.1f", recall)}%  (target ~100%)")
        if (!holes.isEmpty) {
            println("Holes (implemented cards the bridge can't yet cover -> fix mapping/registry):")
            for ((key, n) in holes.mostCommon()) println("  x${n.toString().padEnd(4)} ${key.first} = ${key.second}")
            for ((name, blockers) in blocked) println("    $name: ${blockers.joinToString(",") { it.value }}")
        }
        return 0
    }

    // ---------------------------------------------------------------------------
    // CLI (mirrors probe.py's argparse)
    // ---------------------------------------------------------------------------
    fun run(args: List<String>): Int {
        var setCode: String? = null
        val cardTokens = mutableListOf<String>()
        var calibrate: String? = null
        var show: String? = null
        var refresh = false
        var i = 0
        while (i < args.size) {
            when (val a = args[i]) {
                "--set" -> setCode = args[++i]
                "--calibrate" -> calibrate = args[++i]
                "--card" -> { while (i + 1 < args.size && !args[i + 1].startsWith("--")) cardTokens.add(args[++i]) }
                "--free" -> show = "free"
                "--blocked" -> show = "blocked"
                "--all" -> show = "all"
                "--refresh" -> refresh = true
                else -> { System.err.println("probe: unknown argument $a"); return 2 }
            }
            i++
        }
        val effects = Registry.loadEffectSerialNames()
        val keywords = Registry.loadKeywords()
        return when {
            cardTokens.isNotEmpty() -> modeCard(cardTokens.joinToString(" "), effects, keywords)
            calibrate != null -> modeCalibrate(calibrate, effects, keywords)
            setCode != null -> modeSet(setCode, effects, keywords, show, refresh)
            else -> { System.err.println("probe: one of --set / --card / --calibrate is required"); 2 }
        }
    }
}
