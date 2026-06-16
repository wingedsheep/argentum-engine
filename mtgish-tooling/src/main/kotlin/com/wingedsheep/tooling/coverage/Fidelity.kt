package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.bridge.Bridge
import com.wingedsheep.tooling.coverage.emitter.Emitter
import com.wingedsheep.tooling.coverage.emitter.reprintRowSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.util.Locale

/**
 * GENERATION-fidelity probe — "could we auto-AUTHOR this card?", not merely
 * "is it covered?". Two ground truths: STATIC diffs the emitter's prediction against each card's
 * committed golden snapshot; COMPILED (--gate) diffs the serialised, compiled drafts vs golden.
 */
object Fidelity {
    // Pure structural plumbing — present in the compiled tree but not a game "action". Excluded on
    // BOTH sides so we score what the card DOES.
    private val PLUMBING = setOf(
        "Composite", "GatherCards", "SelectFromCollection", "ChooseUpTo", "ChooseExactly",
        "ForEachInGroup", "ForEachPlayer", "ForEachTarget", "ToZone", "FromZone",
        "TopOfLibrary", "Random",
    )
    private val ALL_SETS = listOf("POR", "INV", "ONS", "KTK", "DOM", "LGN", "SCG", "ARN")

    // ---------------------------------------------------------------------------
    // Snapshot parsing (golden and generated share the "// Name\n{json}" format).
    // ---------------------------------------------------------------------------
    private fun parseBlocks(text: String): Map<String, JsonObject> {
        val out = LinkedHashMap<String, JsonObject>()
        val headerRe = Regex("^// (.+)$", RegexOption.MULTILINE)
        val matches = headerRe.findAll(text).toList()
        for (i in matches.indices) {
            val name = matches[i].groupValues[1].trim()
            val bodyStart = matches[i].range.last + 1
            val bodyEnd = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val body = text.substring(bodyStart, bodyEnd).trim()
            val parsed = runCatching { J.parseToJsonElement(body) as JsonObject }.getOrNull() ?: continue
            out[Cards.front(name)] = parsed
        }
        return out
    }

    private fun parseSnapshot(code: String): Map<String, JsonObject> {
        val path = java.io.File(SNAP_DIR, "${code.uppercase()}.json")
        if (!path.exists()) {
            System.err.println("no golden snapshot at $path — this set has no committed snapshot to diff against")
            kotlin.system.exitProcess(1)
        }
        return parseBlocks(path.readText())
    }

    private fun walkTypes(node: kotlinx.serialization.json.JsonElement?, types: MutableSet<String>, keywords: MutableSet<String>) {
        when (node) {
            is JsonObject -> {
                node["type"].asStr()?.let { types.add(it) }
                node["keywords"].asArr?.forEach { it.asStr()?.let { k -> keywords.add(k) } }
                node.values.forEach { walkTypes(it, types, keywords) }
            }
            is JsonArray -> node.forEach { walkTypes(it, types, keywords) }
            else -> {}
        }
    }

    private fun truthCaps(cardJson: JsonObject, effects: Set<String>): Pair<Set<String>, Set<String>> {
        val types = mutableSetOf<String>(); val keywords = mutableSetOf<String>()
        walkTypes(cardJson, types, keywords)
        return types.filter { it in effects && it !in PLUMBING }.toSet() to keywords
    }

    // Generated side (prediction): what the mtgish->mapping bridge names in Argentum tags. Each tag is
    // resolved through the shared Bridge.resolve so this can't drift from the coverage probe; PLUMBING is
    // stripped here (the fidelity-scoring filter applied symmetrically to truthCaps).
    private fun genCaps(mtgishCard: JsonObject, effects: Set<String>, keywords: Set<String>): Pair<Set<String>, Set<String>> {
        val tags = Counter<Pair<String, String>>()
        Mtgish.extractTags(mtgishCard["Rules"], tags)
        val eff = mutableSetOf<String>(); val kw = mutableSetOf<String>()
        Emitter.findLandwalkKeywords(mtgishCard["Rules"], keywords, kw)
        for ((disc, value) in tags.keys) {
            val r = Bridge.resolve(disc, value, effects, keywords)
            r.effectTag?.let { eff.add(it) }
            r.composedEffects.forEach { if (it !in PLUMBING) eff.add(it) }
            r.keyword?.let { kw.add(it) }
        }
        return eff to kw
    }

    private data class Scored(val tier: String, val recall: Double, val missing: Set<String>)

    private data class TreeDiff(val path: String, val generated: String, val golden: String)
    private data class TreeMismatch(val name: String, val diffs: List<TreeDiff>, val missingCaps: List<String>)

    private fun scoreCard(truth: Pair<Set<String>, Set<String>>, gen: Pair<Set<String>, Set<String>>, complete: Boolean): Scored {
        val tAll = truth.first + truth.second
        val gAll = gen.first + gen.second
        val missing = tAll - gAll
        val recall = if (tAll.isEmpty()) 1.0 else (tAll intersect gAll).size.toDouble() / tAll.size
        val tier = if (missing.isNotEmpty()) "MISS" else if (complete) "AUTO" else "SCAFFOLD"
        return Scored(tier, recall, missing)
    }

    private class SetScore {
        val tiers = mapOf("AUTO" to mutableListOf<String>(), "SCAFFOLD" to mutableListOf(), "MISS" to mutableListOf(), "UNMATCHED" to mutableListOf())
        val missTax = Counter<String>(); val scaffoldReasons = Counter<String>(); val recalls = mutableListOf<Double>()
        // Per-card detail for `--list`: a MISS card's unmapped caps, a SCAFFOLD card's reasons.
        val cardDetail = LinkedHashMap<String, List<String>>()
        var code = ""; var total = 0; var matched = 0
        val avgRecall: Double get() = if (recalls.isEmpty()) 0.0 else recalls.sum() / recalls.size * 100
    }

    /** Capability tiers for one set, vs its committed golden snapshot — null if the set has no snapshot. */
    data class FidelitySummary(
        val matched: Int, val auto: Int, val scaffold: Int, val miss: Int, val unmatched: Int, val avgRecall: Double,
    )

    /**
     * Safe, dashboard-facing summary: returns null (instead of exiting the process) for sets without a
     * committed golden snapshot. Pass a preloaded mtgish [idx] to reuse a shared index pass.
     */
    fun summarizeOrNull(code: String, effects: Set<String>, keywords: Set<String>, idx: Map<String, JsonObject>? = null): FidelitySummary? {
        if (!java.io.File(SNAP_DIR, "${code.uppercase()}.json").exists()) return null
        val s = scoreSet(code, effects, keywords, idx)
        return FidelitySummary(
            matched = s.matched,
            auto = s.tiers["AUTO"]!!.size,
            scaffold = s.tiers["SCAFFOLD"]!!.size,
            miss = s.tiers["MISS"]!!.size,
            unmatched = s.tiers["UNMATCHED"]!!.size,
            avgRecall = s.avgRecall,
        )
    }

    private fun scoreSet(code: String, effects: Set<String>, keywords: Set<String>, idx: Map<String, JsonObject>? = null): SetScore {
        val truth = parseSnapshot(code)
        val mtgish = idx ?: Mtgish.loadMtgishIndex(truth.keys)
        val s = SetScore()
        for (name in truth.keys.sorted()) {
            val mt = mtgish[name]
            if (mt == null) { s.tiers["UNMATCHED"]!!.add(name); continue }
            val t = truthCaps(truth[name]!!, effects)
            val g = genCaps(mt, effects, keywords)
            val res = Emitter.renderCard(mt, Cards.scryfallCard(code, name), effects, keywords)
            val (tier, recall, missing) = scoreCard(t, g, res.complete)
            s.tiers[tier]!!.add(name)
            s.recalls.add(recall)
            missing.forEach { s.missTax.add(it) }
            if (tier == "SCAFFOLD") res.reasons.forEach { s.scaffoldReasons.add(it) }
            if (tier == "MISS") s.cardDetail[name] = missing.sorted()
            else if (tier == "SCAFFOLD") s.cardDetail[name] = res.reasons.sorted()
        }
        s.code = code.uppercase()
        s.total = s.tiers.values.sumOf { it.size }
        s.matched = s.total - s.tiers["UNMATCHED"]!!.size
        return s
    }

    private fun modeAll(effects: Set<String>, keywords: Set<String>): Int {
        println("== cross-set generation fidelity (one bridge, applied unchanged) ==")
        println("   the gap from POR is per-corpus mapping/emitter debt; convergence = one fix helps all\n")
        println(String.format(Locale.ROOT, "  %-5s %7s %6s %9s %6s %7s", "SET", "matched", "AUTO", "SCAFFOLD", "MISS", "recall"))
        println("  " + "-".repeat(46))
        for (code in ALL_SETS) {
            val s = runCatching { scoreSet(code, effects, keywords) }.getOrNull() ?: continue
            val m = if (s.matched == 0) 1 else s.matched
            println(String.format(
                Locale.ROOT, "  %-5s %7d %5.1f%% %8.1f%% %5.1f%% %6.1f%%",
                s.code, s.matched,
                s.tiers["AUTO"]!!.size.toDouble() / m * 100,
                s.tiers["SCAFFOLD"]!!.size.toDouble() / m * 100,
                s.tiers["MISS"]!!.size.toDouble() / m * 100,
                s.avgRecall,
            ))
        }
        return 0
    }

    private fun modeSet(code: String, effects: Set<String>, keywords: Set<String>, listTier: String?): Int {
        val s = scoreSet(code, effects, keywords)
        val matched = s.matched
        println("== ${code.uppercase()} generation fidelity — $matched cards (vs compiled golden) ==\n")
        fun pct(k: String) = if (matched > 0) String.format(Locale.ROOT, "%5.1f%%", s.tiers[k]!!.size.toDouble() / matched * 100) else "  —  "
        println("  AUTO      ${s.tiers["AUTO"]!!.size.toString().padStart(4)}  ${pct("AUTO")}  emitter renders the whole card (recall=1, every action/ability emitted)")
        println("  SCAFFOLD  ${s.tiers["SCAFFOLD"]!!.size.toString().padStart(4)}  ${pct("SCAFFOLD")}  right capabilities, but some structure isn't recovered yet")
        println("  MISS      ${s.tiers["MISS"]!!.size.toString().padStart(4)}  ${pct("MISS")}  bridge omits a capability the card uses")
        println("\n  mean capability recall: ${String.format(Locale.ROOT, "%.1f", s.avgRecall)}%  (effects+keywords matched per card)")
        if (s.tiers["UNMATCHED"]!!.isNotEmpty()) println("  (unmatched in mtgish: ${s.tiers["UNMATCHED"]!!.size})")
        if (!s.missTax.isEmpty) {
            println("\nMISS taxonomy — capabilities the bridge failed to emit (mapping holes to close):")
            for ((cap, c) in s.missTax.mostCommon(20)) println("  x${c.toString().padEnd(4)} $cap")
        }
        if (!s.scaffoldReasons.isEmpty) {
            println("\nSCAFFOLD reasons — structure not recovered (the worklist to push SCAFFOLD->AUTO):")
            for ((r, c) in s.scaffoldReasons.mostCommon(20)) println("  x${c.toString().padEnd(4)} $r")
        }
        if (listTier != null) {
            val names = s.tiers[listTier.uppercase()] ?: emptyList()
            println("\n${listTier.uppercase()} (${names.size}):")
            names.forEach { name ->
                val detail = s.cardDetail[name]
                if (detail.isNullOrEmpty()) println("  - $name")
                else println("  - ${name.padEnd(28)} ${detail.joinToString(", ", "[", "]")}")
            }
        }
        return 0
    }

    private fun emit(name: String, effects: Set<String>, keywords: Set<String>, requestedSet: String? = null): Int {
        val idx = Mtgish.loadMtgishIndex(setOf(name))
        val card = idx[Cards.front(name)] ?: idx.entries.firstOrNull { it.key.lowercase() == Cards.front(name).lowercase() }?.value
        if (card == null) { println("'$name': not found in mtgish IR"); return 1 }
        val cardName = card["Name"].asStr() ?: ""
        // The canonical `card(...)` definition belongs in the card's earliest real-expansion printing, so
        // render its metadata (mana cost, type, collector, artist, image) + package from THAT set — never
        // the asked-for set. Fall back to an implemented set / POR when there's no printing data (offline,
        // no cache) so the emit never regresses to a metadata-less draft.
        val canonical = Printings.earliestRealSet(cardName)?.uppercase()
            ?: Cards.implementedNamesForCard(cardName).firstOrNull()
            ?: "POR"
        val (setCode, scryfall) = Cards.scryfallCard(canonical, cardName)?.let { canonical to it }
            ?: run {
                val fallback = Cards.implementedNamesForCard(cardName).firstOrNull() ?: "POR"
                fallback to Cards.scryfallCard(fallback, cardName)
            }
        val res = Emitter.renderCard(card, scryfall, effects, keywords,
            pkg = "com.wingedsheep.mtg.sets.generated.${setCode.lowercase()}.cards")
        println(res.text)
        // Single-quoted list items, matching the committed golden tier comment format.
        val reasonsRepr = res.reasons.sorted().joinToString(", ", "[", "]") { "'$it'" }
        println("// fidelity tier: ${if (res.complete) "AUTO" else "SCAFFOLD"}" +
            (if (res.reasons.isNotEmpty()) " — unrecovered: $reasonsRepr" else ""))

        // When the caller asked for a specific set that ISN'T the canonical home, that set must contribute
        // only a Printing(...) row (the canonical card(...) above stays in the earliest set's package).
        // Emit it too, so the draft is complete for the requested set — mirroring add-card Step 10b.
        if (requestedSet != null && !requestedSet.equals(setCode, ignoreCase = true)) {
            val target = requestedSet.uppercase()
            val printing = Printings.printingFor(cardName, target)
            val oracleId = printing?.oracleId ?: Printings.printingsOf(cardName).firstNotNullOfOrNull { it.oracleId }
            if (oracleId == null) {
                println("\n// NOTE: no Scryfall printing data for \"$cardName\" in $target — add the Printing(...) row by hand (add-card Step 10b).")
            } else {
                val reprintPkg = "com.wingedsheep.mtg.sets.generated.${target.lowercase()}.cards"
                val meta = Cards.scryfallCard(target, cardName)
                println("\n// ---- reprint row for $target (place in $target's cards/ package; canonical card(...) stays in $setCode) ----")
                print(reprintRowSource(cardName, reprintPkg, setCode, target, oracleId, printing?.releasedAt, meta))
            }
        }
        return 0
    }

    // COMPILED gate — diff the serialised gameplay trees against golden after tiny idiom normalisation.
    private fun modeGate(code: String, effects: Set<String>): Int {
        val genPath = java.io.File(GEN_DIR, "${code.lowercase()}.generated.json")
        if (!genPath.exists()) {
            System.err.println("no serialised drafts at $genPath\nrun the gate first:  ./gradlew :mtg-sets:verifyGeneratedCards -Pset=${code.uppercase()}")
            kotlin.system.exitProcess(1)
        }
        val generated = parseBlocks(genPath.readText())
        val golden = parseSnapshot(code)
        val verified = mutableListOf<String>(); val mismatches = mutableListOf<TreeMismatch>()
        val suspectedGoldenDrift = mutableListOf<TreeMismatch>()
        val capabilityMismatches = mutableListOf<TreeMismatch>()
        for (name in generated.keys.sorted()) {
            if (name !in golden) continue
            val generatedTree = normalizeForFidelity(canonicalizeTargetRefs(generated[name]!!))
            val goldenTree = normalizeForFidelity(canonicalizeTargetRefs(golden[name]!!))
            val missing = missingCapabilities(generated[name]!!, golden[name]!!, effects)
            if (generatedTree == goldenTree) {
                verified.add(name)
            } else {
                val mismatch = TreeMismatch(name, diffTrees(generatedTree, goldenTree), missing)
                if (oracleAssessment(code, mismatch) == OracleAssessment.GOLDEN_DRIFT) {
                    suspectedGoldenDrift.add(mismatch)
                } else {
                    mismatches.add(mismatch)
                }
                if (missing.isNotEmpty()) capabilityMismatches.add(mismatch)
            }
        }
        val notEmitted = (golden.keys - generated.keys).sorted()
        val extraEmitted = (generated.keys - golden.keys).sorted()
        val total = golden.size
        val emittedInGolden = total - notEmitted.size
        println("== ${code.uppercase()} COMPILED gate — generated cards diffed vs golden ==\n")
        println("  AUTO-emitted & COMPILED:   $emittedInGolden/$total  (every emitted card compiled — Gradle gate)")
        println("  GAMEPLAY TREE MATCH:       ${verified.size}  (after tiny known-equivalent normalisation)")
        println("  GAMEPLAY TREE MISMATCH:    ${mismatches.size}  (emitted but not golden-equivalent — must be 0)")
        println("  GOLDEN DRIFT SUSPECTED:    ${suspectedGoldenDrift.size}  (oracle appears to support generated output)")
        println("  capability MISMATCH:       ${capabilityMismatches.size}  (subset missing effect/keyword capabilities)")
        println("  not emitted (left to hand): ${notEmitted.size}")
        if (extraEmitted.isNotEmpty()) println("  emitted outside golden:     ${extraEmitted.size}  (reported, not gated)")
        if (mismatches.isNotEmpty()) {
            println("\nGAMEPLAY TREE MISMATCH taxonomy — first divergent path per emitted card:")
            val buckets = Counter<String>()
            mismatches.forEach { buckets.add(classifyDiff(it.diffs.firstOrNull())) }
            for ((bucket, c) in buckets.mostCommon(20)) println("  x${c.toString().padEnd(4)} $bucket")

            println("\nGAMEPLAY TREE MISMATCH examples — generated != golden:")
            for (m in mismatches.take(30)) {
                val first = m.diffs.firstOrNull()
                val capSuffix = if (m.missingCaps.isEmpty()) "" else " missingCaps=${m.missingCaps}"
                val oracle = Cards.scryfallCard(code, m.name)?.strField("oracle_text")?.replace('\n', ' ')
                if (first == null) {
                    println("  - ${m.name.padEnd(28)} <unknown>$capSuffix")
                } else {
                    val oracleSuffix = if (oracle == null) "" else " oracle=\"${oracle.take(120)}${if (oracle.length > 120) "..." else ""}\""
                    println("  - ${m.name.padEnd(28)} ${first.path}: generated=${first.generated} golden=${first.golden}$capSuffix$oracleSuffix")
                }
            }
            if (mismatches.size > 30) println("  ... ${mismatches.size - 30} more")
        }
        if (suspectedGoldenDrift.isNotEmpty()) {
            println("\nGOLDEN DRIFT SUSPECTED (${suspectedGoldenDrift.size}) — generated differs from golden, but Scryfall oracle supports the generated shape:")
            for (m in suspectedGoldenDrift.take(20)) {
                val first = m.diffs.firstOrNull()
                val oracle = Cards.scryfallCard(code, m.name)?.strField("oracle_text")?.replace('\n', ' ')
                if (first != null) println("  - ${m.name.padEnd(28)} ${first.path}: generated=${first.generated} golden=${first.golden} oracle=\"${oracle ?: ""}\"")
            }
            if (suspectedGoldenDrift.size > 20) println("  ... ${suspectedGoldenDrift.size - 20} more")
        }
        if (notEmitted.isNotEmpty()) {
            println("\nNOT EMITTED (${notEmitted.size}) — engine-feature-complex; the emitter declines rather than emit a wrong card:")
            notEmitted.forEach { println("  - $it") }
        }
        if (extraEmitted.isNotEmpty()) {
            println("\nEMITTED OUTSIDE GOLDEN (${extraEmitted.size}) — generated snapshots with no golden counterpart:")
            extraEmitted.forEach { println("  - $it") }
        }
        val ok = mismatches.isEmpty()
        println("\n${if (ok) "GATE PASS — every emitted card compiles and gameplay-tree matches the golden tree." else "GATE FAIL — an emitted card is not gameplay-equivalent (see GAMEPLAY TREE MISMATCH)."}" +
            "  (${verified.size}/$total of Portal auto-emitted & verified.)")
        return if (ok) 0 else 2
    }

    private fun missingCapabilities(generated: JsonObject, golden: JsonObject, effects: Set<String>): List<String> {
        val g = truthCaps(generated, effects)
        val t = truthCaps(golden, effects)
        return ((t.first + t.second) - (g.first + g.second)).sorted()
    }

    /**
     * Canonicalize target-reference labels before diffing. A target's bound-variable name and the matching
     * requirement `id` are an arbitrary binding key — the engine resolves `BoundVariable` purely by
     * name→requirement-id lookup (`namedTargets[name]`), so the string itself is never gameplay-visible.
     * Two gameplay-equivalent cards routinely differ ONLY in that label: golden authors pick descriptive
     * names ("target creature to destroy", "any", "creature"), while the emitter emits a generic "target".
     *
     * A card with a single logical target collapses its label to "target", reusing [normalizeForFidelity]'s
     * legacy convention so it stays interchangeable with `ContextTarget(0) -> TargetRef`. A card with
     * several distinct labels gets positional tokens instead (first-appearance order), so the binding
     * structure — which effect references which requirement — is preserved and a genuinely reordered
     * binding still diverges. Filters, counts, and effect shape are untouched: a real filter/structure bug
     * (e.g. `powerAtLeast` vs `powerOrToughnessAtLeast`) still surfaces as a mismatch.
     */
    private fun canonicalizeTargetRefs(root: JsonElement): JsonElement {
        val labels = LinkedHashSet<String>()
        fun collect(node: JsonElement?) {
            when (node) {
                is JsonObject -> {
                    if (node["type"].asStr() == "BoundVariable") node["name"].asStr()?.let { labels.add(it) }
                    node.values.forEach { collect(it) }
                }
                is JsonArray -> node.forEach { collect(it) }
                else -> {}
            }
        }
        collect(root)
        val mapping: Map<String, String> = when {
            labels.isEmpty() -> emptyMap()
            labels.size == 1 -> mapOf(labels.first() to "target")
            else -> labels.withIndex().associate { (i, l) -> l to "§T$i§" }
        }
        // A target-requirement node carries an `id` (the binding key) and a target `type`. Its id is
        // gameplay-relevant ONLY when some BoundVariable references it by name (handled via `mapping`);
        // a requirement referenced positionally (`ContextTarget`/`ContextPlayer` index) leaves its id a
        // pure decoration the golden author still names descriptively ("target player"). Collapse such an
        // unreferenced id to "target" so [normalizeForFidelity] then drops it, matching the emitter's
        // generic "target" (Desperate Bloodseeker). Never touches a referenced (`mapping`) id, so the
        // multi-target binding-reorder check is preserved.
        fun isTargetRequirementId(node: JsonObject): Boolean =
            node.containsKey("id") && node["type"].asStr()?.contains("Target") == true
        fun rewrite(node: JsonElement): JsonElement = when (node) {
            is JsonArray -> JsonArray(node.map { rewrite(it) })
            is JsonObject -> {
                val isBound = node["type"].asStr() == "BoundVariable"
                val isReqNode = isTargetRequirementId(node)
                JsonObject(node.entries.associate { (k, v) ->
                    when {
                        isBound && k == "name" -> k to (mapping[v.asStr()]?.let { JsonPrimitive(it) } ?: rewrite(v))
                        k == "id" && v.asStr() in mapping -> k to JsonPrimitive(mapping.getValue(v.asStr()!!))
                        k == "id" && isReqNode -> k to JsonPrimitive("target")  // unreferenced binding key
                        else -> k to rewrite(v)
                    }
                })
            }
            else -> node
        }
        return rewrite(root)
    }

    private fun normalizeForFidelity(node: JsonElement): JsonElement {
        if (node is JsonArray) return JsonArray(node.map { normalizeForFidelity(it) })
        if (node !is JsonObject) return node

        val type = node["type"].asStr()
        if (type == "BoundVariable" && node["name"].asStr() == "target") {
            return JsonObject(mapOf("type" to JsonPrimitive("TargetRef")))
        }
        if (type == "ContextTarget" && node["index"].asInt() == 0) {
            return JsonObject(mapOf("type" to JsonPrimitive("TargetRef")))
        }
        if (type == "Gated" && node["gate"].asStr() == "Gate.MayDecide" && node.containsKey("then")) {
            return JsonObject(mapOf(
                "type" to JsonPrimitive("OptionalEffect"),
                "then" to normalizeForFidelity(node.getValue("then")),
            ))
        }
        if ((node["optional"] as? JsonPrimitive)?.booleanOrNull == true && node.isAbilityNode() && node.containsKey("effect")) {
            return JsonObject(node.entries
                .filterNot { (key, _) -> key == "optional" || key in NON_GAMEPLAY_KEYS || key == "id" }
                .associate { (key, value) ->
                    key to if (key == "effect") {
                        JsonObject(mapOf(
                            "type" to JsonPrimitive("OptionalEffect"),
                            "then" to normalizeForFidelity(value),
                        ))
                    } else {
                        normalizeForFidelity(value)
                    }
                })
        }
        if (type == "AggregateBattlefield" && node["player"].asStr() == "Each" &&
            (node["aggregation"].asStr() == null || node["aggregation"].asStr() == "COUNT") &&
            node["property"] == null && node["excludeSelf"] == null) {
            val fields = linkedMapOf<String, JsonElement>(
                "type" to JsonPrimitive("Count"),
                "player" to JsonPrimitive("Each"),
                "zone" to JsonPrimitive("Battlefield"),
            )
            node["filter"]?.let { fields["filter"] = normalizeForFidelity(it) }
            return JsonObject(fields)
        }
        if (node.keys == setOf("type", "id") && node["id"].asStr() == "target") {
            node["type"].asStr()?.let { return JsonPrimitive(it) }
        }
        return JsonObject(node.entries
            .filterNot { (key, value) ->
                key in NON_GAMEPLAY_KEYS ||
                    (key == "id" && (node.isAbilityNode() || value.asStr() == "target"))
            }
            .associate { (key, value) -> key to normalizeForFidelity(value) })
    }

    // `imageUri` is purely cosmetic art (a created token's picture, like a card's), never a rules input —
    // the same reason `oracleText`/`metadata` are excluded. The emitter doesn't resolve a token's Scryfall
    // image, so a hand-authored card carrying it would diverge on art alone; ignore it like the others.
    // `descriptionOverride` is the human-readable rules-text override a hand-authored ability may carry
    // (`description = "…"`); it's display-only (never a rules input, like `oracleText`), and the emitter
    // doesn't synthesise per-ability prose — so ignore it rather than diverge on text alone.
    private val NON_GAMEPLAY_KEYS = setOf("metadata", "oracleText", "colorIdentityOverride", "imageUri", "descriptionOverride")

    private fun JsonObject.isAbilityNode(): Boolean {
        return containsKey("effect") && (containsKey("trigger") || containsKey("cost"))
    }

    private fun diffTrees(generated: JsonElement, golden: JsonElement, max: Int = 8): List<TreeDiff> {
        val out = mutableListOf<TreeDiff>()
        fun walk(g: JsonElement?, t: JsonElement?, path: String) {
            if (out.size >= max || g == t) return
            when {
                g is JsonObject && t is JsonObject -> {
                    val keys = (g.keys + t.keys).sorted()
                    for (key in keys) {
                        if (out.size >= max) return
                        walk(g[key], t[key], "$path.$key")
                    }
                }
                g is JsonArray && t is JsonArray -> {
                    val n = maxOf(g.size, t.size)
                    for (i in 0 until n) {
                        if (out.size >= max) return
                        walk(g.getOrNull(i), t.getOrNull(i), "$path[$i]")
                    }
                }
                else -> out.add(TreeDiff(path, shortJson(g), shortJson(t)))
            }
        }
        walk(generated, golden, "\$")
        return out
    }

    private fun shortJson(node: JsonElement?): String {
        val raw = if (node == null) "<missing>" else J.encodeToString(JsonElement.serializer(), node)
        return if (raw.length <= 120) raw else raw.take(117) + "..."
    }

    private fun classifyDiff(diff: TreeDiff?): String {
        val path = diff?.path ?: return "unknown"
        return when {
            ".targetRequirement" in path || ".targetRequirements" in path -> "target/filter recovery"
            ".target" in path -> "effect target wiring"
            ".amount" in path || ".count" in path -> "amount/count recovery"
            ".condition" in path || ".gate" in path -> "condition/gate recovery"
            ".keywords" in path -> "keyword recovery"
            ".metadata" in path || ".colorIdentityOverride" in path -> "metadata recovery"
            ".script" in path -> "effect/ability structure"
            else -> path.removePrefix("\$.").substringBefore('.')
        }
    }

    private enum class OracleAssessment { GOLDEN_DRIFT, UNKNOWN }

    private fun oracleAssessment(code: String, mismatch: TreeMismatch): OracleAssessment {
        val first = mismatch.diffs.firstOrNull() ?: return OracleAssessment.UNKNOWN
        val oracle = Cards.scryfallCard(code, mismatch.name)?.strField("oracle_text")?.lowercase(Locale.ROOT) ?: return OracleAssessment.UNKNOWN
        if ("target player or planeswalker" in oracle &&
            first.generated.contains("TargetPlayerOrPlaneswalker") && first.golden.contains("TargetPlayer")) {
            return OracleAssessment.GOLDEN_DRIFT
        }
        if ("target opponent or planeswalker" in oracle &&
            first.generated.contains("TargetOpponentOrPlaneswalker") && first.golden.contains("TargetOpponent")) {
            return OracleAssessment.GOLDEN_DRIFT
        }
        if ("target player" in oracle && "target opponent" !in oracle &&
            first.generated.contains("TargetPlayer") && first.golden.contains("TargetOpponent")) {
            return OracleAssessment.GOLDEN_DRIFT
        }
        if ("target player gains" in oracle &&
            first.path.endsWith(".target") && first.generated.contains("TargetRef") && first.golden == "<missing>") {
            return OracleAssessment.GOLDEN_DRIFT
        }
        if (mismatch.diffs.all { it.path.contains(".castRestrictions") } && first.generated == "<missing>" &&
            "cast only" !in oracle && "activate only" !in oracle) {
            return OracleAssessment.GOLDEN_DRIFT
        }
        if ("you may" in oracle && first.generated == "<missing>" &&
            (first.path.endsWith(".effect.destination") || first.path.endsWith(".effect.target") ||
                first.path.endsWith(".effect.effects") || first.path.endsWith(".effect.byDestruction"))) {
            return OracleAssessment.GOLDEN_DRIFT
        }
        return OracleAssessment.UNKNOWN
    }

    fun run(args: List<String>): Int {
        var setCode: String? = null
        var all = false
        val emitTokens = mutableListOf<String>()
        var gate: String? = null
        var listTier: String? = null
        var i = 0
        while (i < args.size) {
            when (val a = args[i]) {
                "--set" -> setCode = args[++i]
                "--all" -> all = true
                "--gate" -> gate = args[++i]
                "--emit" -> { while (i + 1 < args.size && !args[i + 1].startsWith("--")) emitTokens.add(args[++i]) }
                "--list" -> listTier = args[++i]
                else -> { System.err.println("fidelity: unknown argument $a"); return 2 }
            }
            i++
        }
        val effects = Registry.loadEffectSerialNames()
        val keywords = Registry.loadKeywords()
        return when {
            emitTokens.isNotEmpty() -> emit(emitTokens.joinToString(" "), effects, keywords, setCode)
            all -> modeAll(effects, keywords)
            gate != null -> modeGate(gate, effects)
            setCode != null -> modeSet(setCode, effects, keywords, listTier)
            else -> { System.err.println("fidelity: one of --set / --all / --emit / --gate is required"); 2 }
        }
    }
}
