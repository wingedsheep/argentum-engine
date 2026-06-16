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
        Regex("""(?<!Conditions\.)\bEntityMatches\s*\(""") to
            "Conditions.EntityMatches(...) (or Conditions.SourceMatches/TargetMatchesFilter/…)",
    )

    test("card definitions construct effects/costs via the Effects/Costs facades, not raw types") {
        val definitionsDir = Paths.get(
            "src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
        ).toAbsolutePath()

        val violations = mutableListOf<String>()

        Files.walk(definitionsDir).use { stream ->
            stream.filter { it.name.endsWith(".kt") }.forEach { path ->
                stripCommentsAndImports(path.readText()).forEachIndexed { idx, line ->
                    for ((regex, hint) in forbidden) {
                        if (regex.containsMatchIn(line)) {
                            val rel = definitionsDir.parent.parent.relativize(path)
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
