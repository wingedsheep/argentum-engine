package com.wingedsheep.tooling.coverage.emitter

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Pins the emitter's source-formatting helpers: long-line wrapping (so generated cards match the
 * hand-authored house style) and typed-subtype rendering (so filters use `Subtype.X`, not strings).
 */
class EmitterFormatTest : StringSpec({

    "short lines are left untouched" {
        val line = "        effect = Effects.DrawCards(1)"
        wrapLine(line) shouldContainExactly listOf(line)
    }

    "a long call wraps its top-level args one per line, with trailing-comma-free last arg" {
        val line = "        effect = Effects.ForEachInGroup(GroupFilter(GameObjectFilter.Creature." +
            "withKeyword(Keyword.FLYING)), DealDamageEffect(4, EffectTarget.Self))"
        wrapLine(line) shouldContainExactly listOf(
            "        effect = Effects.ForEachInGroup(",
            "            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),",
            "            DealDamageEffect(4, EffectTarget.Self)",
            "        )",
        )
    }

    "commas inside string literals never trigger a split" {
        val line = "        flavorText = \"" + "x".repeat(130) + ", and more\""
        // No code-level parens -> the line is returned unchanged despite the comma in the string.
        wrapLine(line) shouldContainExactly listOf(line)
    }

    "comment / KDoc lines are never wrapped" {
        val kdoc = " * " + "Oracle text that runs very long ".repeat(6)
        wrapLine(kdoc) shouldContainExactly listOf(kdoc)
    }

    "nested over-long args wrap recursively and every emitted line fits the width" {
        val inner = (1..12).joinToString(", ") { "DealDamageEffect($it, EffectTarget.Self)" }
        val line = "        effect = Effects.Composite(Effects.ForEachInGroup(GroupFilter.AllCreatures, " +
            "Effects.Composite($inner)))"
        val out = wrapLine(line, maxWidth = 80)
        out.forEach { it.length shouldBeLessThanOrEqual 80 }
    }

    "subtypeArg uses the typed constant when the SDK names it, else falls back to a string" {
        subtypeArg("Plains") shouldBe "Subtype.PLAINS"          // Subtype.kt: val PLAINS = Subtype("Plains")
        subtypeArg("Zombiefied Whatsit 9000") shouldBe "\"Zombiefied Whatsit 9000\""  // no such constant
    }

    "subtypeCtorArg keeps the argument a Subtype even when the SDK doesn't name it" {
        subtypeCtorArg("Plains") shouldBe "Subtype.PLAINS"
        subtypeCtorArg("Zombiefied Whatsit 9000") shouldBe "Subtype(\"Zombiefied Whatsit 9000\")"
    }

    // Guard for the class of bug that let adding `Subtype.MOUNT` to the SDK (while implementing three
    // Aetherdrift cards) break an unrelated emitter test. `withSubtype` is overloaded on `Subtype` and
    // `String`, so a hand-rolled `"\"$sub\""` compiles and behaves identically to the typed constant —
    // nothing fails until someone adds a constant and flips that site's output from under a pinned
    // expectation. Routing every site through subtypeArg/subtypeCtorArg makes the rendering a single
    // decision; this test keeps it that way, because the next hand-rolled literal is invisible otherwise.
    "no emitter source hand-rolls a subtype literal instead of subtypeArg / subtypeCtorArg" {
        val offenders = EMITTER_SOURCES
            .filter { it.name !in SUBTYPE_RENDERER_FILES }
            .flatMap { f ->
                f.readLines().withIndex()
                    .filter { (_, line) -> HAND_ROLLED_SUBTYPE.containsMatchIn(codeOf(line)) }
                    .map { (i, line) -> "${f.name}:${i + 1}  ${line.trim()}" }
            }
        withClue(
            "Hand-rolled subtype literal(s). Render via subtypeArg(value) — or subtypeCtorArg(value) " +
                "where the parameter is typed `Subtype` — so the typed/string choice lives in one place:\n" +
                offenders.joinToString("\n"),
        ) { offenders.shouldBeEmpty() }
    }
})

private val EMITTER_SOURCES =
    File("src/main/kotlin/com/wingedsheep/tooling/coverage").walkTopDown().filter { it.extension == "kt" }.toList()

/** The two renderers that are *allowed* to spell the literal forms out — they define them. */
private val SUBTYPE_RENDERER_FILES = setOf("Shells.kt")

/**
 * `withSubtype("…")` / `notSubtype(Subtype("…"))` and friends built from a raw string rather than the
 * shared renderer. `withAnySubtype` is deliberately absent: its SDK parameter is `vararg String`, so a
 * quoted literal is the only thing that compiles there.
 */
private val HAND_ROLLED_SUBTYPE =
    Regex("""(withSubtype|notSubtype|HasSubtype|withAnyOfSubtypes)\s*\(\s*(Subtype\s*\(\s*)?\\?"""")

/**
 * The code part of a line — KDoc and comments routinely *describe* the literal forms (that's how the
 * helpers document themselves), and flagging prose would make this lint unrunnable.
 */
private fun codeOf(line: String): String {
    val t = line.trimStart()
    if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) return ""
    return line.substringBefore("//")
}
