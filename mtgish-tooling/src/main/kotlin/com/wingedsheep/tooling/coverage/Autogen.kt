package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import com.wingedsheep.tooling.coverage.emitter.RenderResult
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.util.Locale

/**
 * Auto-generation gap detector + draft generator (port of autogen.py), on top of the mtgish bridge.
 *
 *  --gaps SET     bucket a set's UNIMPLEMENTED cards into AUTOGEN / SCAFFOLD / BLOCKED + leaderboard
 *  --write SET    emit a draft `.kt` per AUTOGEN missing card into a STAGING dir for human review
 *  --emit-all SET emit every whole-renderable card (impl included) for the Kotlin compile gate
 *  --write-all SET replace a real set's card sources with mtgish-generated files, scaffolds included
 *  --all          with --gaps: sweep every Scryfall booster set, sum the AUTOGEN total
 *
 * Drafts stay in staging: they're predictions from approximate IR; ground truth stays a
 * human-authored card whose scenario test passes. Deliberately NOT a card loader.
 */
object Autogen {
    private fun genPackage(setCode: String) = "com.wingedsheep.mtg.sets.generated.${setCode.lowercase()}.cards"
    private fun draftPackage(setCode: String) = "com.wingedsheep.mtg.sets.definitions.${setCode.lowercase()}.cards"
    private fun sourceFileName(name: String) = name.replace(Regex("[^A-Za-z0-9]"), "") + ".kt"

    private fun isBasicLand(card: JsonObject): Boolean {
        val typeline = card["Typeline"]
        val supertypes = typeline.field("Supertypes").asArr?.mapNotNull { it.asStr() } ?: emptyList()
        val cardtypes = typeline.field("Cardtypes").asArr?.mapNotNull { it.asStr() } ?: emptyList()
        return "Basic" in supertypes && "Land" in cardtypes
    }

    private fun isBasicLandSource(file: File): Boolean =
        file.isFile && file.extension == "kt" && Regex("\\bbasicLand\\s*\\(").containsMatchIn(file.readText())

    private fun render(card: JsonObject, setCode: String, effects: Set<String>, keywords: Set<String>, pkg: String): RenderResult {
        val scryfall = Cards.scryfallCard(setCode, card["Name"].asStr() ?: "")
        return Emitter.renderCard(card, scryfall, effects, keywords, pkg = pkg)
    }

    private fun classify(card: JsonObject, setCode: String, effects: Set<String>, keywords: Set<String>): String {
        if (!Probe.analyze(card, effects, keywords).coverable) return "BLOCKED"
        return if (render(card, setCode, effects, keywords, genPackage(setCode)).complete) "AUTOGEN" else "SCAFFOLD"
    }

    private fun missingWithMtgish(setCode: String): Pair<List<String>, Map<String, JsonObject>> {
        val (draft, extra) = Cards.canonicalNames(setCode)
        if (draft == null || extra == null) {
            System.err.println("no Scryfall data for ${setCode.uppercase()} — run: just card-status --set ${setCode.uppercase()}")
            kotlin.system.exitProcess(1)
        }
        val impl = Cards.implementedNames(setCode)
        val missing = ((draft + extra) - impl).sorted()
        return missing to Mtgish.loadMtgishIndex(missing.toSet())
    }

    private fun allWithMtgish(setCode: String): Pair<List<String>, Map<String, JsonObject>> {
        val (draft, extra) = Cards.canonicalNames(setCode)
        if (draft == null || extra == null) {
            System.err.println("no Scryfall data for ${setCode.uppercase()} — run: just card-status --set ${setCode.uppercase()}")
            kotlin.system.exitProcess(1)
        }
        val names = (draft + extra).sorted()
        return names to Mtgish.loadMtgishIndex(names.toSet())
    }

    private data class Classified(val missing: List<String>, val cats: Map<String, MutableList<String>>, val blockTax: Counter<String>)

    private fun classifyMissing(setCode: String, effects: Set<String>, keywords: Set<String>): Classified {
        val (missing, idx) = missingWithMtgish(setCode)
        val cats = mapOf("AUTOGEN" to mutableListOf<String>(), "SCAFFOLD" to mutableListOf(), "BLOCKED" to mutableListOf(), "UNMATCHED" to mutableListOf())
        val blockTax = Counter<String>()
        for (name in missing) {
            val card = idx[name]
            if (card == null) { cats["UNMATCHED"]!!.add(name); continue }
            val cat = classify(card, setCode, effects, keywords)
            cats[cat]!!.add(name)
            if (cat == "BLOCKED") for (b in Probe.analyze(card, effects, keywords).blockers) blockTax.add(b.value)
        }
        return Classified(missing, cats, blockTax)
    }

    private fun modeGaps(setCode: String, effects: Set<String>, keywords: Set<String>, listCat: String?): Int {
        val (missing, cats, blockTax) = classifyMissing(setCode, effects, keywords)
        val n = missing.size
        println("== ${setCode.uppercase()} auto-generation gap — $n unimplemented cards ==\n")
        println("  AUTOGEN   ${cats["AUTOGEN"]!!.size.toString().padStart(4)}   emitter renders a whole compiling card now")
        println("  SCAFFOLD  ${cats["SCAFFOLD"]!!.size.toString().padStart(4)}   covered, but structure needs hand-wiring")
        println("  BLOCKED   ${cats["BLOCKED"]!!.size.toString().padStart(4)}   capability gap (needs mapping or engine work)")
        if (cats["UNMATCHED"]!!.isNotEmpty()) println("  (unmatched in mtgish: ${cats["UNMATCHED"]!!.size} — name join / Un-set / too new)")
        println("\n  -> `just coverage-generate --set ${setCode.uppercase()}` drafts the ${cats["AUTOGEN"]!!.size} AUTOGEN cards into a staging dir.")
        if (!blockTax.isEmpty) {
            println("\nBLOCKED leaderboard — capability ranked by # cards it would unlock:")
            for ((cap, c) in blockTax.mostCommon(15)) println("  x${c.toString().padEnd(4)} $cap")
        }
        if (listCat != null) {
            val names = cats[listCat.uppercase()] ?: emptyList<String>()
            println("\n${listCat.uppercase()} (${names.size}):")
            names.forEach { println("  - $it") }
        }
        return 0
    }

    private fun modeWrite(setCode: String, effects: Set<String>, keywords: Set<String>, outdir: String?): Int {
        val (missing, idx) = missingWithMtgish(setCode)
        val out = if (outdir != null) File(outdir) else File(DEFAULT_GENERATED_ROOT, setCode.lowercase())
        out.mkdirs()
        var written = 0
        for (name in missing) {
            val card = idx[name] ?: continue
            if (isBasicLand(card)) continue
            if (!Probe.analyze(card, effects, keywords).coverable) continue
            val res = render(card, setCode, effects, keywords, draftPackage(setCode))
            if (!res.complete) continue
            File(out, sourceFileName(name)).writeText(res.text)
            written++
        }
        println("wrote $written draft card(s) to $out")
        println("These are DRAFTS — compile, add a scenario test, and review before moving into the set.")
        return 0
    }

    private fun modeEmitAll(setCode: String, effects: Set<String>, keywords: Set<String>, outdir: String?): Int {
        val (names, idx) = allWithMtgish(setCode)
        val out = if (outdir != null) File(outdir) else File(DEFAULT_GENERATED_ROOT, setCode.lowercase())
        if (out.exists()) out.listFiles { f -> f.name.endsWith(".kt") }?.forEach { it.delete() }  // fresh dir
        out.mkdirs()
        var written = 0
        var skippedBasicLands = 0
        for (name in names) {
            val card = idx[name] ?: continue
            if (isBasicLand(card)) { skippedBasicLands++; continue }
            val res = render(card, setCode, effects, keywords, genPackage(setCode))
            if (!res.complete) continue
            File(out, sourceFileName(name)).writeText(res.text)
            written++
        }
        println("emit-all: wrote $written/${names.size} whole-card drafts to $out (package ${genPackage(setCode)})")
        if (skippedBasicLands > 0) println("  skipped basic lands: $skippedBasicLands (use curated basicLand definitions)")
        return 0
    }

    private fun modeWriteAll(setCode: String, effects: Set<String>, keywords: Set<String>, outdir: String?): Int {
        val (names, idx) = allWithMtgish(setCode)
        val out = if (outdir != null) File(outdir) else File(DEFINITIONS_ROOT, "${setCode.lowercase()}/cards")
        if (out.exists()) out.listFiles { f -> f.extension == "kt" }?.forEach { if (!isBasicLandSource(it)) it.delete() }
        out.mkdirs()
        var written = 0
        var scaffold = 0
        var unmatched = 0
        var skippedBasicLands = 0
        for (name in names) {
            val card = idx[name]
            if (card == null) { unmatched++; continue }
            if (isBasicLand(card)) { skippedBasicLands++; continue }
            val res = render(card, setCode, effects, keywords, draftPackage(setCode))
            if (!res.complete) scaffold++
            File(out, sourceFileName(name)).writeText(res.text)
            written++
        }
        println("write-all: wrote $written/${names.size} card source file(s) to $out (package ${draftPackage(setCode)})")
        if (scaffold > 0) println("  scaffolds: $scaffold (compileable source with STRUCTURE comments; behaviour incomplete)")
        if (unmatched > 0) println("  unmatched in mtgish: $unmatched")
        if (skippedBasicLands > 0) println("  skipped basic lands: $skippedBasicLands (preserved existing basicLand definitions)")
        return 0
    }

    private fun modeGapsAll(effects: Set<String>, keywords: Set<String>, listCat: String?, unique: Boolean): Int {
        val codes = Cards.allSetCodes()
        System.err.println("== auto-generation gap across ${codes.size} Scryfall booster sets ==")
        System.err.println("   (corpus is name-keyed oracle IR — every set is reasoned over, not just a sample)\n")
        data class Row(val code: String, val missing: Int, val autogen: Int, val scaffold: Int, val blocked: Int, val unmatched: Int)
        val rows = mutableListOf<Row>()
        val totals = Counter<String>()
        val autogenUnion = mutableSetOf<String>()
        for ((i, code) in codes.withIndex()) {
            System.err.print("  [${(i + 1).toString().padStart(3)}/${codes.size}] $code ...\r")
            if (Cards.canonicalNames(code).first == null) continue  // no cache and fetch failed — skip
            val (missing, cats, _) = classifyMissing(code, effects, keywords)
            if (missing.isEmpty()) continue
            cats["AUTOGEN"]!!.forEach { autogenUnion.add(Cards.front(it)) }
            rows.add(Row(code, missing.size, cats["AUTOGEN"]!!.size, cats["SCAFFOLD"]!!.size, cats["BLOCKED"]!!.size, cats["UNMATCHED"]!!.size))
            for (k in listOf("AUTOGEN", "SCAFFOLD", "BLOCKED", "UNMATCHED")) totals.add(k, cats[k]!!.size)
            totals.add("MISSING", missing.size)
            if (listCat != null) {
                val names = cats[listCat.uppercase()] ?: emptyList<String>()
                if (names.isNotEmpty()) {
                    System.err.println("\n$code ${listCat.uppercase()} (${names.size}):")
                    names.forEach { System.err.println("  - $it") }
                }
            }
        }
        System.err.print(" ".repeat(40) + "\r")  // clear progress line

        rows.sortWith(compareByDescending<Row> { it.autogen }.thenByDescending { it.missing })
        println(String.format(Locale.ROOT, "  %-5s %7s %7s %8s %7s %9s", "SET", "missing", "AUTOGEN", "SCAFFOLD", "BLOCKED", "UNMATCHED"))
        println("  " + "-".repeat(54))
        for (r in rows) println(String.format(Locale.ROOT, "  %-5s %7d %7d %8d %7d %9d", r.code, r.missing, r.autogen, r.scaffold, r.blocked, r.unmatched))
        println("  " + "-".repeat(54))
        println(String.format(Locale.ROOT, "  %-5s %7d %7d %8d %7d %9d", "TOTAL", totals["MISSING"], totals["AUTOGEN"], totals["SCAFFOLD"], totals["BLOCKED"], totals["UNMATCHED"]))
        println("\n  ${totals["AUTOGEN"]} unimplemented cards across ${rows.size} sets would auto-author a whole compiling card today.")
        println("  (per-set count — a reprint counts once per set, and once per already-authored printing)")

        if (unique) {
            val distinct = autogenUnion.size
            val netNew = (autogenUnion - Cards.allImplementedNames()).sorted()
            println("\n  NET-NEW (deduped across sets, minus everything already implemented anywhere):")
            println("    ${distinct.toString().padStart(6)}  distinct AUTOGEN card names (cross-set duplicates collapsed)")
            println("    ${netNew.size.toString().padStart(6)}  genuinely unimplemented — the real auto-authorable backlog")
            println("\n  NET-NEW cards (${netNew.size}):")
            netNew.forEach { println("    - $it") }
        } else {
            println("  (--unique collapses cross-set reprints + already-implemented cards into a net-new count)")
        }
        println("\n  Per set: `just coverage-generate --set <CODE>` drafts its AUTOGEN cards into staging.")
        return 0
    }

    fun run(args: List<String>): Int {
        var setCode: String? = null
        var all = false
        var gaps = false; var write = false; var emitAll = false; var writeAll = false
        var listCat: String? = null
        var unique = false
        var out: String? = null
        var i = 0
        while (i < args.size) {
            when (val a = args[i]) {
                "--set" -> setCode = args[++i]
                "--all" -> all = true
                "--gaps" -> gaps = true
                "--write" -> write = true
                "--emit-all" -> emitAll = true
                "--write-all" -> writeAll = true
                "--list" -> listCat = args[++i]
                "--unique" -> unique = true
                "--out" -> out = args[++i]
                else -> { System.err.println("autogen: unknown argument $a"); return 2 }
            }
            i++
        }
        val effects = Registry.loadEffectSerialNames()
        val keywords = Registry.loadKeywords()
        if (unique && !all) { System.err.println("autogen: --unique only applies to --all"); return 2 }
        if (all) {
            if (write || emitAll || writeAll) { System.err.println("autogen: --all is only supported with --gaps"); return 2 }
            return modeGapsAll(effects, keywords, listCat, unique)
        }
        if (setCode == null) { System.err.println("autogen: --set CODE (or --all) is required"); return 2 }
        return when {
            writeAll -> modeWriteAll(setCode, effects, keywords, out)
            write -> modeWrite(setCode, effects, keywords, out)
            emitAll -> modeEmitAll(setCode, effects, keywords, out)
            else -> modeGaps(setCode, effects, keywords, listCat)
        }
    }
}
