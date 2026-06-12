package com.wingedsheep.mtg.sets

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
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
        Regex("""\bCompositeEffect\s*\(""") to "Effects.Composite(...)",
        Regex("""\bMoveToZoneEffect\s*\(""") to "Effects.Move(...) (or Effects.Destroy/Exile/ReturnToHand/…)",
        Regex("""\bForEachInGroupEffect\s*\(""") to "Effects.ForEachInGroup(...)",
        Regex("""\bAdditionalCost\.[A-Z]""") to "Costs.additional.*",
        Regex("""\bPayCost\.[A-Z]""") to "Costs.pay.*",
    )

    /**
     * Raw pipeline-step constructors. Card files compose Gather → Select → Move pipelines through
     * the `Effects.Pipeline { }` builder (or `Effects.PipelineSteps { }` for the few effects that
     * take a bare `List<Effect>` of steps), which threads typed slot handles instead of hand-written
     * string keys (see `backlog/inline-pipeline-dsl.md` §5 step 4, `docs/card-sdk-language-reference.md`
     * §5.5). The builder serializes to the exact same step tree, so this ban is a pure
     * authoring-surface boundary — the raw `@Serializable` step types remain the JSON/custom-card path.
     */
    val pipelineStepHint = "Effects.Pipeline { } (or Effects.PipelineSteps { } for ForEach* lists)"
    val pipelineStepForbidden = listOf(
        "GatherCardsEffect", "GatherUntilMatchEffect", "GatherSubtypesEffect",
        "SelectFromCollectionEffect", "SelectTargetEffect", "MoveCollectionEffect",
        "FilterCollectionEffect", "RevealCollectionEffect", "CaptureControllersEffect",
        "ForEachCapturedControllerEffect", "StoreCardNameEffect", "StoreNumberEffect",
        "ChoosePileEffect", "ConditionalOnCollectionEffect", "ChooseOptionEffect",
        "NoteCreatureTypeEffect",
    ).map { Regex("""\b$it\s*\(""") to pipelineStepHint }

    /**
     * Files exempt from the pipeline-step ban only — each holds a raw step in a shape the builder
     * cannot reproduce byte-identically, so converting it would churn the snapshot golden. All
     * OTHER facade rules still apply to these files. Keep this list short and justified; a new
     * pipeline card should never land here.
     *
     *  - A single step as a `ConditionalEffect` branch (`effect`/`elseEffect`) — not a multi-step
     *    pipeline; wrapping it in `Effects.Pipeline { }` would add a `CompositeEffect` node.
     *      AetherRift, BreOfClanStoutarm, CelestialReunion, WhiskervaleForerunner
     *  - A `.then(...)` chain whose leftmost element is an opaque pattern-composite
     *    (`Patterns.Library.scry`, `Patterns.Hand.putFromHand`, …); `then` flattens that composite's
     *    children, so the builder can't reproduce the flat list without inlining the pattern.
     *      ElvenFarsight, MeekAttack, Dermoplasm, TerminalVelocity, CauldronDance
     *  - A `ConditionalOnCollectionEffect` with an empty-`Composite` branch — `ifNotEmpty { }`/
     *    `orElse { }` require a non-empty body.
     *      PulsarSquadronAce, ManholeMissile
     *  - A reused `val steps = listOf(<raw steps>)` concatenated across modes — the list must stay a
     *    raw value, not a builder result.
     *      CruelclawsHeist
     */
    val pipelineStepAllowlist = setOf(
        "inv/cards/AetherRift.kt",
        "ecl/cards/BreOfClanStoutarm.kt",
        "ecl/cards/CelestialReunion.kt",
        "blb/cards/WhiskervaleForerunner.kt",
        "ltr/cards/ElvenFarsight.kt",
        "ecl/cards/MeekAttack.kt",
        "lgn/cards/Dermoplasm.kt",
        "eoe/cards/TerminalVelocity.kt",
        "inv/cards/CauldronDance.kt",
        "eoe/cards/PulsarSquadronAce.kt",
        "tmt/cards/ManholeMissile.kt",
        "blb/cards/CruelclawsHeist.kt",
    )

    test("card definitions construct effects/costs via the Effects/Costs facades, not raw types") {
        val definitionsDir = Paths.get(
            "src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
        ).toAbsolutePath()

        val violations = mutableListOf<String>()

        Files.walk(definitionsDir).use { stream ->
            stream.filter { it.name.endsWith(".kt") }.forEach { path ->
                val rel = definitionsDir.parent.parent.relativize(path)
                val pipelineExempt = pipelineStepAllowlist.any { rel.toString().endsWith(it) }
                val rules = if (pipelineExempt) forbidden else forbidden + pipelineStepForbidden
                stripCommentsAndImports(path.readText()).forEachIndexed { idx, line ->
                    for ((regex, hint) in rules) {
                        if (regex.containsMatchIn(line)) {
                            violations += "$rel:${idx + 1}  →  use $hint instead of `${regex.find(line)!!.value}`"
                        }
                    }
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
