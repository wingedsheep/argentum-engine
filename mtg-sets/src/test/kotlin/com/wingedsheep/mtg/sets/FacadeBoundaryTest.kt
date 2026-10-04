package com.wingedsheep.mtg.sets

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.readText

/**
 * Enforces the facade boundary (SDK architecture review §2.3).
 *
 * Card definitions are an anti-corruption layer: they must construct effects and costs through
 * the curated `Effects.*` / `Costs.*` facades, never through the foundational data classes
 * directly. That contract is what lets the SDK refactor those underlying types without touching
 * the ~3,500 card files. This test scans every card definition and fails on any direct
 * construction of the foundational effect/cost types, pointing at the facade to use instead.
 *
 * Comments and `import` lines are ignored — only real code is checked.
 */
class FacadeBoundaryTest : FunSpec({

    /** Forbidden construction → human-readable facade hint. */
    val forbidden = listOf(
        Regex("""\bCompositeEffect\s*\(""") to "`a then b` (Effects.Composite(...) for a computed list)",
        Regex("""\bMoveToZoneEffect\s*\(""") to "Effects.Move(...) (or Effects.Destroy/Exile/ReturnToHand/…)",
        Regex("""\bForEachInGroupEffect\s*\(""") to "Effects.ForEachInGroup(...)",
        Regex("""\bAdditionalCost\.[A-Z]""") to "Costs.additional.*",
        Regex("""\bPayCost\.[A-Z]""") to "Costs.pay.*",
        Regex("""(?<!Conditions\.)\bEntityMatches\s*\(""") to
            "Conditions.EntityMatches(...) (or Conditions.SourceMatches/TargetMatchesFilter/…)",
        // Every effect data class is named `…Effect`; a card never constructs one directly. The
        // lookbehind lets qualified calls through — `Effects.GrantReplacementEffect(…)`,
        // `ModalEffect.chooseOne(…)` — and objects (`SacrificeSelfEffect`) aren't constructions.
        Regex("""((?<![\w.])|\bscripting\.effects\.)[A-Z]\w*Effect\s*\(""") to "the Effects.* facade for that effect",
        Regex("""(?<![\w.])(GatedEffect\s*\(|Gate\.[A-Z])""") to
            "Effects.If / May / MayPay / MayPayX / IfYouDo",
        // Amounts go through DynamicAmounts (and the `+ - * /` operators); most facades that take an
        // amount also take an Int, so a constant needs no wrapper at all.
        Regex("""\bDynamicAmount\.[A-Z]""") to "DynamicAmounts.* (or an Int / an operator)",
        Regex("""(?<![\w.])Compare\s*\(""") to "Conditions.CompareAmounts(...)",
        // A trigger is a subject and a verb; the subject (self / attached / a / another / oneOrMore /
        // a player) fixes the binding, so a card never spells one.
        Regex("""(?<![\w.])TriggerSpec\s*\(""") to "Triggers.<subject>.<verb>()",
        Regex("""\bTriggerBinding\.""") to "the Triggers subject (self / attached / a(…) / another(…))",
        Regex("""\.copy\(\s*binding\s*=""") to "the Triggers subject (self / attached / a(…) / another(…))",
    )

    /**
     * Raw pipeline steps thread their data through string keys, so a typo or a read of a
     * collection nobody wrote only surfaces at runtime (or in `CardLinter`). Cards write pipelines
     * with `Effects.Pipeline { }`, whose steps return typed handles; the raw steps stay
     * SDK-internal (the `Patterns.*` helpers, the engine, JSON-loaded cards).
     */
    val pipelineSteps = listOf(
        "GatherCardsEffect", "SelectFromCollectionEffect", "MoveCollectionEffect", "FilterCollectionEffect",
        "RevealCollectionEffect", "ConditionalOnCollectionEffect", "GatherUntilMatchEffect",
        "GatherSubtypesEffect", "ChoosePileEffect", "CaptureControllersEffect", "ForEachCapturedControllerEffect",
        "StoreCardNameEffect", "StoreNumberEffect", "SelectTargetEffect", "ChooseOptionEffect",
        "ChooseOnePerCategoryEffect", "NoteCreatureTypeEffect", "PairWithSourceEffect",
        "CopyCardIntoCollectionEffect", "CopyCollectionIntoCollectionEffect",
    ).map { Regex("""(?<![\w.])$it\s*\(""") to "Effects.Pipeline { … } (the typed step verbs)" } + listOf(
        Regex("""\b(storeAs|storeSelected|storeRemainder|storeMatching|storeNonMatching|storeMatch|storeRevealed|storeChosenAs|storeOtherAs|storeMovedAs|collectionName|storeDestroyedAs|storeExiledAs)\s*=\s*"""") to
            "a handle from Effects.Pipeline { } (or runStoringCollection { key -> … } for a non-step writer)",
        Regex("""VariableReference\(\s*"\w*_count"\s*\)""") to "CollectionSlot.count",
        Regex("""\.key\b(?!\s*=)""") to "the handle itself or a typed accessor (count, asSource, asTarget, controllerOf)",
    )

    /**
     * Cards that must still spell a pipeline key, each with the reason. Keep this short; a new
     * entry needs a reason a reviewer would accept.
     */
    val pipelineAllowlist: Map<String, String> = mapOf(
        "rav/cards/Flickerform.kt" to
            "CreateDelayedTriggerEffect.carryCollections names the collections the delayed trigger remembers",
        "lgn/cards/PlanarGuide.kt" to
            "CreateDelayedTriggerEffect.carryCollections names the collections the delayed trigger remembers",
        "mh3/cards/GlimpseTheImpossible.kt" to
            "CreateDelayedTriggerEffect.carryCollections names the collections the delayed trigger remembers",
        "mh3/cards/PheliaExuberantShepherd.kt" to
            "CreateDelayedTriggerEffect.carryCollections names the collections the delayed trigger remembers",
        "mh3/cards/WheelOfPotential.kt" to
            "the per-player May runs its own pipeline, so its tracked move names the slot the enclosing " +
                "forEachPlayerCollecting aggregates — a handle minted inside the May lambda can't be returned out of it",
        "dsk/cards/MonstrousEmergence.kt" to
            "the cost's ChooseEntity storeAs is read by the spell effect — a cost is not inside any pipeline",
        "eoe/cards/CloseEncounter.kt" to
            "the cost's ChooseEntity storeAs is read by the spell effect — a cost is not inside any pipeline",
    )

    test("card definitions write pipelines with Effects.Pipeline, not raw string-keyed steps") {
        val violations = mutableListOf<String>()

        SetSourceRoots.definitionFiles().forEach { path ->
            val rel = SetSourceRoots.relativize(path)
            if (pipelineAllowlist.keys.any { rel.toString().endsWith(it) }) return@forEach
            stripCommentsAndImports(path.readText()).forEachIndexed { idx, line ->
                for ((regex, hint) in pipelineSteps) {
                    if (regex.containsMatchIn(line)) {
                        violations += "$rel:${idx + 1}  →  use $hint instead of `${regex.find(line)!!.value}`"
                    }
                }
            }
        }

        withClue(
            "Card definitions must write pipelines through Effects.Pipeline { } (typed handles, no string keys).\n" +
                violations.joinToString("\n")
        ) {
            violations shouldBe emptyList()
        }
    }

    /**
     * A card holds the targets it reads — `val creature = target(TargetFilter.Creature)`, a mode's
     * or reflexive trigger's own `target(…)`, `handle.asPlayer` for a player-typed slot — rather
     * than counting positions with `ContextTarget(i)` / `Player.ContextPlayer(i)`, which silently
     * shift when a requirement is added, made optional or reordered.
     *
     * The one exception is the body of `Effects.ForEachTarget(…)`: the engine runs it once per
     * chosen target with the target list rebound to that one target, so `ContextTarget(0)` there
     * *is* "the current target" — the handle of the whole requirement would be the wrong object.
     */
    test("card definitions read their targets through named handles, not positions") {
        val positional = Regex("""\b(ContextTarget|ContextPlayer)\s*\(""")
        val violations = mutableListOf<String>()

        SetSourceRoots.definitionFiles().forEach { path ->
            val code = blankStringLiterals(stripCommentsAndImports(path.readText()).joinToString("\n"))
            val perTarget = spansOf(code, "Effects.ForEachTarget(")
            positional.findAll(code).forEach { match ->
                if (perTarget.none { match.range.first in it }) {
                    val line = code.substring(0, match.range.first).count { it == '\n' } + 1
                    violations += "${SetSourceRoots.relativize(path)}:$line  →  use a named target handle " +
                        "instead of `${match.value}`"
                }
            }
        }

        withClue(
            "Card definitions must read targets through named handles (target(…) / targets(…) / .asPlayer).\n" +
                violations.joinToString("\n")
        ) {
            violations shouldBe emptyList()
        }
    }

    /**
     * One spelling per concept, for the two things nearly every card writes:
     *
     * - **A target** is `target(filter)` / `targets(filter, count = …)` for an object and
     *   `target(Targets.X)` for any other shape. The handle's binding id is minted by the DSL and
     *   the prompt the player sees is derived from the requirement, so a card never names one, and
     *   never spells the long form `target(TargetObject(…))` the filter overloads already cover.
     * - **A sequence** is `a then b then c`. `Effects.Composite(…)` stays for a *computed* list (one
     *   built with `map`, or carrying a `descriptionOverride`); an empty one is `Effects.Nothing`.
     */
    test("card definitions spell targets and sequences one way") {
        val rules = listOf(
            Regex("""(?<![\w.])(target|targets|kickerTarget|cleaveTarget)\(\s*TargetObject\(""") to
                "target(TargetFilter…, optional = …) / targets(TargetFilter…, count = …)",
            Regex("""(?<![\w.])(target|targets|kickerTarget|cleaveTarget)\(\s*"""") to
                "target(requirement) — the DSL mints the binding id and derives the prompt",
            Regex("""(?<![\w.])(TargetPlayer|TargetOpponent|AnyTarget|TargetCreatureOrPlaneswalker|TargetPlayerOrPlaneswalker|TargetOpponentOrPlaneswalker|TargetCreatureOrPlayer|TargetPermanentOrPlayer)\(\s*\)""") to
                "the Targets.* preset",
            Regex("""\.then\(""") to "infix `a then b`",
            Regex("""\bEffects\.Composite\(\s*(listOf\(|Effects\.|Patterns\.|emptyList\(|\))""") to
                "`a then b then c` (or Effects.Nothing)",
        )
        val violations = mutableListOf<String>()

        SetSourceRoots.definitionFiles().forEach { path ->
            val code = blankStringLiterals(stripCommentsAndImports(path.readText()).joinToString("\n"))
            for ((regex, hint) in rules) {
                regex.findAll(code).forEach { match ->
                    val line = code.substring(0, match.range.first).count { it == '\n' } + 1
                    violations += "${SetSourceRoots.relativize(path)}:$line  →  use $hint instead of `${match.value.trim()}`"
                }
            }
        }

        withClue("Card definitions must spell targets and sequences one way.\n" + violations.joinToString("\n")) {
            violations shouldBe emptyList()
        }
    }

    test("card definitions construct effects/costs via the Effects/Costs facades, not raw types") {
        val violations = mutableListOf<String>()

        SetSourceRoots.definitionFiles().forEach { path ->
            stripCommentsAndImports(path.readText()).forEachIndexed { idx, line ->
                // The specific hints come first; one report per line is enough.
                forbidden.firstOrNull { (regex, _) -> regex.containsMatchIn(line) }?.let { (regex, hint) ->
                    val rel = SetSourceRoots.relativize(path)
                    violations += "$rel:${idx + 1}  →  use $hint instead of `${regex.find(line)!!.value}`"
                }
            }
        }

        withClue(
            "Card definitions must go through the Effects.*/Costs.* facades (SDK review §2.3).\n" +
                violations.joinToString("\n")
        ) {
            violations shouldBe emptyList()
        }
    }
})

/**
 * Returns the file's lines with block comments, line comments, and `import` declarations blanked
 * out (line numbers preserved), so the scan only sees executable code.
 */
internal fun stripCommentsAndImports(source: String): List<String> {
    val out = ArrayList<String>()
    var inBlock = false
    for (raw in source.lines()) {
        if (raw.trimStart().startsWith("import ")) {
            out += ""
            continue
        }
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (inBlock) {
                if (i + 1 < raw.length && raw[i] == '*' && raw[i + 1] == '/') {
                    inBlock = false; i += 2
                } else i++
            } else {
                if (i + 1 < raw.length && raw[i] == '/' && raw[i + 1] == '*') {
                    inBlock = true; i += 2
                } else if (i + 1 < raw.length && raw[i] == '/' && raw[i + 1] == '/') {
                    break
                } else {
                    sb.append(raw[i]); i++
                }
            }
        }
        out += sb.toString()
    }
    return out
}

/** [code] with the contents of every string literal replaced by spaces (length preserved). */
internal fun blankStringLiterals(code: String): String {
    val out = StringBuilder(code)
    var i = 0
    while (i < code.length) {
        if (code.startsWith("\"\"\"", i)) {
            val end = code.indexOf("\"\"\"", i + 3).let { if (it < 0) code.length else it }
            for (k in i + 3 until end) if (out[k] != '\n') out[k] = ' '
            i = end + 3
        } else if (code[i] == '"') {
            var k = i + 1
            while (k < code.length && code[k] != '"' && code[k] != '\n') {
                if (code[k] == '\\') k++
                k++
            }
            for (j in i + 1 until minOf(k, code.length)) out[j] = ' '
            i = k + 1
        } else i++
    }
    return out.toString()
}

/** The index ranges of every `[opener]…)` call in [code] (opener ends with `(`), paren-matched. */
internal fun spansOf(code: String, opener: String): List<IntRange> {
    val spans = mutableListOf<IntRange>()
    var from = code.indexOf(opener)
    while (from >= 0) {
        var depth = 0
        var k = from + opener.length - 1
        while (k < code.length) {
            when (code[k]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> { depth--; if (depth == 0) break }
            }
            k++
        }
        spans += from..k
        from = code.indexOf(opener, from + opener.length)
    }
    return spans
}
