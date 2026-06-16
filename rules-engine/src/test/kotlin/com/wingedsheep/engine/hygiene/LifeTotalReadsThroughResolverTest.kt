package com.wingedsheep.engine.hygiene

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File

/**
 * Guards the Two-Headed Giant shared-life invariant (CR 810.9a): an individual player's life total
 * is the team's shared total, so every life *value* read and write must go through the
 * [com.wingedsheep.engine.state.GameState] resolver (`lifeTotal` / `withLifeTotal` / `adjustLife`),
 * never directly off a player's `LifeTotalComponent`. A raw `LifeTotalComponent(...)` write or a
 * `.get<LifeTotalComponent>()?.life` read on a non-canonical teammate would silently desync a team.
 *
 * Presence checks (`get<LifeTotalComponent>() != null` / `== null`, used to detect "is this a
 * player") are fine — every player still carries the component — so this guard only flags the
 * `.life` value accessor and the `LifeTotalComponent(...)` constructor.
 *
 * If this fails: replace the read with `state.lifeTotal(playerId)` and the write with
 * `state.withLifeTotal(playerId, newLife)` (or `state.adjustLife(playerId, delta)`).
 */
class LifeTotalReadsThroughResolverTest : FunSpec({

    test("life value reads/writes go through the GameState resolver, not raw LifeTotalComponent") {
        val valueRead = Regex("""LifeTotalComponent>\(\)[?!.]*\.life""")
        val construct = Regex("""\bLifeTotalComponent\s*\(""")

        val offenders = sourceRoot().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val relative = file.absolutePath.replace('\\', '/')
                    .substringAfter("src/main/kotlin/")
                if (relative in ALLOWED_FILES) return@flatMap emptySequence<String>()
                file.readLines().withIndex().mapNotNull { (idx, line) ->
                    if (valueRead.containsMatchIn(line) || construct.containsMatchIn(line)) {
                        "$relative:${idx + 1}: ${line.trim()}"
                    } else null
                }.asSequence()
            }
            .toList()

        offenders.shouldBeEmpty()
    }
}) {
    companion object {
        /**
         * Files that legitimately touch the raw component. Each is the authoritative read/write
         * surface the resolver itself routes through — they must not be "fixed" to call the
         * resolver (that would be infinite recursion or a chicken-and-egg at init).
         */
        private val ALLOWED_FILES = setOf(
            // The resolver itself + team-life ownership helpers read/write the canonical component.
            "com/wingedsheep/engine/state/GameState.kt",
            // Game setup stamps the initial LifeTotalComponent on every player entity.
            "com/wingedsheep/engine/core/GameInitializer.kt",
            // The component's own declaration.
            "com/wingedsheep/engine/state/components/identity/PlayerIdentityComponents.kt",
        )

        private fun sourceRoot(): File =
            listOf(File("src/main/kotlin"), File("rules-engine/src/main/kotlin"))
                .firstOrNull { it.isDirectory }
                ?: error("Could not locate rules-engine/src/main/kotlin from ${File(".").absolutePath}")
    }
}
