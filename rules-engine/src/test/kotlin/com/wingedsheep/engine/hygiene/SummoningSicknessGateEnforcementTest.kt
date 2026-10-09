package com.wingedsheep.engine.hygiene

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File

/**
 * Guards the `{T}`/`{Q}` activation gate: every place that asks "is this creature's tap/untap cost
 * blocked by summoning sickness?" must ask
 * [com.wingedsheep.engine.mechanics.SummoningSicknessRules], not open-code
 * `has<SummoningSicknessComponent>() && !hasKeyword(HASTE)`.
 *
 * The open-coded form was duplicated at fourteen sites across the mana solver, both ability
 * enumerators, the cost helpers and the activation handler. Any permission that lifts the ability
 * half of CR 302.6 without granting haste — currently
 * [com.wingedsheep.sdk.core.AbilityFlag.MAY_ACTIVATE_ABILITIES_AS_THOUGH_HASTY] (Thousand-Year
 * Elixir, Shang-Chi) — has to be honoured at *all* of them or the enumerator and the authoritative
 * re-check disagree and the ability is offered but rejected (or vice versa). Centralizing makes the
 * next such permission a one-line change; this test makes a new bypass fail the build.
 *
 * [ALLOWED_FILES] lists the files that read the component directly, each for a reason that is *not*
 * a tap/untap gate: the marker's own lifecycle, the attack half of CR 302.6 (which as-though-hasty
 * deliberately does not lift), the separate
 * [com.wingedsheep.sdk.scripting.ActivationRestriction.ControlledSinceYourMostRecentTurn]
 * restriction, and the client's display badge.
 *
 * The scan matches both `has<…>` and `get<…>` spellings of the read, so `get<…>() != null` is not
 * an escape hatch.
 *
 * **Known limit, stated plainly:** the allowlist is per *file*, not per line, so a new open-coded
 * tap gate added inside **any** of the eight [ALLOWED_FILES] would not be caught — the biggest
 * (`ManaSolver.kt`) as much as `LegalityKernel.kt` and `SacrificeAndPayContinuationResumer.kt`. Every *other* file in
 * `rules-engine/src/main/kotlin` is covered. The scan also does not cover `ai/`, `game-server/`,
 * `gym/` or `mtg-sets/` at all; the reads there are attack-evaluation heuristics and scenario setup.
 * A per-line allowlist (file → a regex the permitted line must match, e.g.
 * `ControlledSinceYourMostRecentTurn` / `canAttack`) would close most of the eight without new
 * machinery, and is the obvious next step if this ever catches nothing while a bypass ships.
 */
class SummoningSicknessGateEnforcementTest : FunSpec({

    test("tap/untap summoning-sickness gates go through SummoningSicknessRules") {
        val offenders = findDirectSicknessReads(sourceRoot())
            .filterNot { it.relativePath in ALLOWED_FILES }

        offenders.map { "${it.relativePath}:${it.lineNumber}: ${it.line.trim()}" }.shouldBeEmpty()
    }
}) {
    companion object {

        /**
         * A direct read of the marker: `has<SummoningSicknessComponent>()` or
         * `get<SummoningSicknessComponent>()`, qualified or not. Both spellings, because
         * `get<…>() != null` is the same gate written differently and matching only `has` would let
         * it through in **any** file rather than just the allowlisted ones — a hole the per-file
         * granularity documented above does not cover.
         */
        private val DIRECT_READ_PATTERN =
            Regex("""\.(?:has|get)<\s*(?:[\w.]+\.)?SummoningSicknessComponent\s*>""")

        /**
         * Files permitted to read [com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent]
         * directly. None of these is a `{T}`/`{Q}` activation gate.
         */
        private val ALLOWED_FILES = setOf(
            // The shared gate itself.
            "com/wingedsheep/engine/mechanics/SummoningSicknessRules.kt",
            // Marker lifecycle: the untap step clears it for the active player's permanents.
            "com/wingedsheep/engine/core/BeginningPhaseManager.kt",
            // Same lifecycle, for an effect that grants an extra untap/turn mid-resolution.
            "com/wingedsheep/engine/handlers/continuations/SacrificeAndPayContinuationResumer.kt",
            // The *attack* half of CR 302.6 (CR 702.10b). Reads plain haste on purpose — an
            // "activate as though hasty" grant must never make a creature able to attack.
            "com/wingedsheep/engine/mechanics/combat/rules/AttackRestrictionRules.kt",
            // Enlist expressly requires continuous control or actual haste. It is not a {T}/{Q}
            // activation cost; the as-though-hasty activation permission must not qualify a helper.
            "com/wingedsheep/engine/mechanics/combat/EnlistAttackCosts.kt",
            // A non-gate read: ManaSource.canAttack, an auto-tap preference that models attacking,
            // so plain haste is correct. See the class KDoc's "known limit".
            "com/wingedsheep/engine/mechanics/mana/ManaSolver.kt",
            // ActivationRestriction.ControlledSinceYourMostRecentTurn — a printed activation
            // restriction generalized beyond creatures; haste does not lift it (CR 702.10c covers
            // only the tap/untap symbols). The legality kernel is the one place that decides it,
            // for the activation handler, the enumerators and auto-tap alike.
            "com/wingedsheep/engine/legality/LegalityKernel.kt",
            // The client's "summoning sick" badge, which reports attack-readiness.
            "com/wingedsheep/engine/view/projection/CardProjector.kt",
        )

        private data class DirectRead(
            val relativePath: String,
            val lineNumber: Int,
            val line: String
        )

        /** Resolves `rules-engine/src/main/kotlin` from either the module root or the repo root. */
        private fun sourceRoot(): File {
            val candidates = listOf(
                File("src/main/kotlin"),
                File("rules-engine/src/main/kotlin")
            )
            return candidates.firstOrNull { it.isDirectory }
                ?: error("Could not locate rules-engine/src/main/kotlin from ${File(".").absolutePath}")
        }

        private fun findDirectSicknessReads(sourceRoot: File): List<DirectRead> {
            val rootPath = sourceRoot.absolutePath.replace('\\', '/')
            val results = mutableListOf<DirectRead>()
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val relative = file.absolutePath.replace('\\', '/').removePrefix("$rootPath/")
                    file.useLines { lines ->
                        lines.forEachIndexed { idx, raw ->
                            val code = stripLineComment(raw)
                            if (code.trimStart().startsWith("*")) return@forEachIndexed
                            if (DIRECT_READ_PATTERN.containsMatchIn(code)) {
                                results += DirectRead(relative, idx + 1, raw)
                            }
                        }
                    }
                }
            return results
        }

        /** Drops a trailing `// …` line comment so commented-out examples don't trip the scan. */
        private fun stripLineComment(line: String): String {
            val idx = line.indexOf("//")
            return if (idx >= 0) line.substring(0, idx) else line
        }
    }
}
