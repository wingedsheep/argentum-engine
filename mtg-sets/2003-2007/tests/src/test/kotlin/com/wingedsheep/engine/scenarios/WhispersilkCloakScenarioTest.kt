package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Whispersilk Cloak — "Equipped creature can't be blocked and has shroud."
 *
 * The evasion half used to be `flags(AbilityFlag.CANT_BE_BLOCKED)`, which lands it on the Equipment
 * rather than on the creature it equips, so the equipped creature was blockable as normal while the
 * shroud half — a proper `GrantKeyword` static — worked. Argentum Assay's combat-restriction band
 * reported the mismatch.
 */
class WhispersilkCloakScenarioTest : ScenarioTestBase() {

    init {
        context("the equipped creature cannot be blocked") {

            test("no creature may block the equipped attacker") {
                val game = combatScenario()

                val result = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant")))

                withClue("Whispersilk Cloak must make the creature it equips unblockable") {
                    result.error shouldNotBe null
                }
            }
        }
    }

    private fun combatScenario() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardAttachedTo(1, "Whispersilk Cloak", "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()
        .also { game ->
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }
}
